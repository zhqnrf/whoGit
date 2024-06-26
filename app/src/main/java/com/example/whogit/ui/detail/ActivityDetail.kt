package com.example.whogit.ui.detail

import android.content.res.ColorStateList
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import coil.load
import coil.transform.CircleCropTransformation
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.example.whogit.R
import com.example.whogit.data.model.DetailUserModel
import com.example.whogit.data.model.UserModel
import com.example.whogit.database.ConfigDatabase
import com.example.whogit.databinding.ActivityDetailBinding
import com.example.whogit.ui.follow.FollowFragment
import com.example.whogit.ui.follow.PageAdapter
import com.example.whogit.ui.utils.Result

class DetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDetailBinding
    private val viewModel by viewModels<DetailViewModel>{
        DetailViewModel.Factory(ConfigDatabase(this))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val item = intent.getParcelableExtra<UserModel.ItemsItem>("item")
        val username = item?.login ?: ""

        getListItemData(item)
        getTabLayoutData(username)
        getFavoriteButton(item)
        isButtonOnFavorite(username,item?.id ?: 0)
        binding.btnBack.setOnClickListener {
            onBackPressed()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            android.R.id.home -> {
                finish()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    /*
    setupViews
     */
    private fun getListItemData(user : UserModel.ItemsItem?){
        user?.let { item ->
            val username = item.login
            viewModel.getDetailUser(username)
            viewModel.getFollowers(username)

            viewModel.resultDetailUser.observe(this){
                when(it){
                    is Result.Success<*> -> {
                        val userItem = it.data as DetailUserModel
                        binding.ivProfile.load(userItem.avatarUrl){
                            transformations(CircleCropTransformation())
                        }

                        binding.tvName.text = userItem.login
                        binding.tvUsername.text = userItem.name

                        val followersText = getString(R.string.followersTemplate, userItem.followers.toString())
                        val followingText = getString(R.string.followingTemplate, userItem.following.toString())

                        binding.tvTotalFollowers.text = followersText
                        binding.tvTotalFollowing.text = followingText
                    }
                    is Result.Error -> {
                        Toast.makeText(this, it.exception.message.toString(), Toast.LENGTH_SHORT).show()
                    }
                    is Result.Loading -> {
                        binding.progressBar.isVisible = it.isLoading
                    }
                }
            }
        }
    }

    /*
    setupTabLayout
     */

    private fun getTabLayoutData(username: String){

        val fragment = mutableListOf<Fragment>(
            FollowFragment.newInstance(FollowFragment.FOLLOWERS),
            FollowFragment.newInstance(FollowFragment.FOLLOWING)
        )

        val titleFragment = mutableListOf(
            getString(R.string.tab_text_1),
            getString(R.string.tab_text_2)
        )
        val adapter = PageAdapter(this, fragment)
        binding.viewPager.adapter = adapter

        TabLayoutMediator(binding.tabLayout, binding.viewPager){tab, position ->
            tab.text = titleFragment[position]
        }.attach()

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener{
            override fun onTabSelected(tab: TabLayout.Tab?) {
                if (tab?.position == 0){
                    viewModel.getFollowers(username)
                }else{
                    viewModel.getFollowing(username)
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
            }

        })
    }
    /*
    observeDatabaseChanges
     */
    private fun isButtonOnFavorite(username: String, userId: Int){
        viewModel.userInDatabase.observe(this){userInDB ->
            val color = if (userInDB) R.color.colorPrimary else R.color.colorThirdly
            binding.btnFavorite.changeIconColor(color)
        }
        viewModel.checkFavorite(userId){
            binding.btnFavorite.changeIconColor(R.color.colorThirdly)
        }

        viewModel.isSuccessAddFav.observe(this){
            binding.btnFavorite.changeIconColor(R.color.colorPrimary)
        }

        viewModel.DeleteFav.observe(this){
            binding.btnFavorite.changeIconColor(R.color.colorThirdly)
        }

        viewModel.checkingUser(userId)
    }

    private fun getFavoriteButton(user: UserModel.ItemsItem?){
        binding.btnFavorite.setOnClickListener{
            viewModel.setButtonFav(user)
        }
    }

}
/*
  changeIconColor
   */
fun FloatingActionButton.changeIconColor(colorRes: Int){
   imageTintList = ColorStateList.valueOf(ContextCompat.getColor(this.context,colorRes))
}
