package common.ui.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import common.ui.pages.components.RyderRed

@Composable
fun SearchPage() {

    var query by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf(SearchTab.USERS) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(top = 24.dp)
    ) {

        // Page header
        Text(
            text = "Search",
            color = RyderRed,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Search field
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Search accounts or #hashtags") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Tabs: Accounts / Hashtags
        TabRow(
            selectedTabIndex = selectedTab.ordinal,
            containerColor = Color.Black,
            contentColor = RyderRed
        ) {
            SearchTab.values().forEach { tab ->
                Tab(
                    selected = selectedTab == tab,
                    onClick = { selectedTab = tab },
                    text = {
                        Text(
                            text = tab.title,
                            color = if (selectedTab == tab) RyderRed else Color.Gray
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Results list
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            when (selectedTab) {
                SearchTab.USERS -> {
                    items(dummyUsers(query)) { user ->
                        SearchRow(
                            title = user,
                            subtitle = "View profile"
                        )
                    }
                }
                SearchTab.HASHTAGS -> {
                    items(dummyHashtags(query)) { tag ->
                        SearchRow(
                            title = "#$tag",
                            subtitle = "View posts"
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchRow(title: String, subtitle: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { }
            .padding(horizontal = 24.dp, vertical = 12.dp)
    ) {
        Text(
            text = title,
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = subtitle,
            color = Color.Gray,
            fontSize = 12.sp
        )
    }
}

private enum class SearchTab(val title: String) {
    USERS("Accounts"),
    HASHTAGS("Hashtags")
}

private fun dummyUsers(query: String): List<String> {
    val users = listOf("IronRider", "NightWolf", "ThrottleKing", "RoadQueen", "MotoLife")
    return users.filter { it.contains(query, ignoreCase = true) }
}

private fun dummyHashtags(query: String): List<String> {
    val tags = listOf("rideout", "bikelife", "openroad", "twowheels", "motoculture")
    return tags.filter { it.contains(query, ignoreCase = true) }
}
