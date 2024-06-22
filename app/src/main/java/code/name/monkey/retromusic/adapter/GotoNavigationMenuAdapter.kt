package code.name.monkey.retromusic.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.navOptions
import androidx.recyclerview.widget.RecyclerView
import code.name.monkey.retromusic.EXTRA_ALBUM_ID
import code.name.monkey.retromusic.EXTRA_ARTIST_ID
import code.name.monkey.retromusic.R
import code.name.monkey.retromusic.fragments.base.goToArtist
import code.name.monkey.retromusic.helper.MusicPlayerRemote

class GotoNavigationMenuAdapter(
    private val navController: NavController,
    private val drawerLayout: DrawerLayout,
    private val items: List<Triple<Int, String, Int>>
) : RecyclerView.Adapter<GotoNavigationMenuAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_navigation, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val itemView = holder.itemView
        val icon = itemView.findViewById<ImageView>(R.id.icon)
        val title = itemView.findViewById<TextView>(R.id.title)

        val (iconRes, titleText, actionId) = items[position]
        icon.setImageResource(iconRes)
        title.text = titleText
        itemView.setOnClickListener {
            navigate(actionId)
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START);
            } else if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
                drawerLayout.closeDrawer(GravityCompat.END);
            }
        }
    }

    override fun getItemCount(): Int {
        return items.size
    }

    fun navigate(actionId: Int){
        val currentSong = MusicPlayerRemote.currentSong
        when (actionId){
            R.id.action_go_to_album -> {
                navController.navigate(
                    R.id.albumDetailsFragment,
                    bundleOf(EXTRA_ALBUM_ID to currentSong.albumId)
                )
            }

            R.id.action_go_to_artist -> {
                navController.navigate(
                    R.id.artistDetailsFragment,
                    bundleOf(EXTRA_ARTIST_ID to currentSong.artistId)
                )
            }

            R.id.action_go_to_genre -> {
                /*
                navController.navigate(
                    R.id.genreDetailsFragment,
                    bundleOf(EXTRA_GENRE_ID to currentSong.genreId)
                )
                 */
            }

            R.id.action_go_to_lyrics -> {
                navController.navigate(
            R.id.lyrics_fragment,
            null,
            navOptions { launchSingleTop = true }
        )
            }
        }
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view)
}
