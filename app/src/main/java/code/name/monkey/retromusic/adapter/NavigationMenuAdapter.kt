package code.name.monkey.retromusic.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.recyclerview.widget.RecyclerView
import code.name.monkey.retromusic.R
import com.google.android.material.navigation.NavigationView

class NavigationMenuAdapter(private val navController: NavController, private val drawerLayout: DrawerLayout) : RecyclerView.Adapter<NavigationMenuAdapter.ViewHolder>() {

    private val data = (1..50).map { "item number $it" }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_navigation, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val itemView = holder.itemView
        val icon = itemView.findViewById<ImageView>(R.id.icon)
        val title = itemView.findViewById<TextView>(R.id.title)

        val itemNumber = data[position].substringAfter("item number ").substringBefore(" ").toInt()

        title.text = data[position]

        itemView.setOnClickListener {
            if (itemNumber % 2 == 0) {
                navController.navigate(R.id.action_album)
                drawerLayout.closeDrawer(GravityCompat.START)
            } else {
                navController.navigate(R.id.action_folder)
                drawerLayout.closeDrawer(GravityCompat.START)
            }
        }
    }

    override fun getItemCount(): Int {
        return data.size
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view)
}
