package amazon.fetch.test

import amazon.fetch.test.ui.theme.AmazonFetchTestTheme
import android.content.ClipData.Item
import android.os.Bundle
import android.util.JsonReader
import android.util.JsonToken
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.InputStreamReader
import java.net.URL
import javax.net.ssl.HttpsURLConnection

private lateinit var jsonReader : JsonReader
private lateinit var myConnection : HttpsURLConnection
private var fullListOfItemHolders: List<ItemHolder> = mutableListOf()
private var listOfItemHoldersByListId: MutableList<List<ItemHolder>> = mutableListOf()

data class ItemHolder (
    var id: Int = 0,
    var listId: Int = 0,
    var name: String = ""
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AmazonFetchTestTheme {
                MainLayout()
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun MainLayout() {
        Scaffold(
            modifier = Modifier,
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = colorResource(R.color.grey_800),
                        titleContentColor = Color.White,
                    ),
                    title = {
                        Text("Fetch Test!")
                    },
                )
            },
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding),
            ) {
                ListIdTabs()
            }
        }
    }
}

@Composable
fun ListIdTabs() {
    val tabs = listOf("Full List", "ListID 1", "ListID 2", "ListID 3", "ListID 4")
    var tabIndex by remember { mutableIntStateOf(0) }

    TabRow(selectedTabIndex = tabIndex) {
        tabs.forEachIndexed { index, title ->
            Tab(text = { Text(title) },
                selected = tabIndex == index,
                onClick = {
                    tabIndex = index
                }
            )
        }
    }

    ItemList(tabIndex)
}


@Composable
fun ItemList(tabIndex: Int) {
    //Simple remembering state so our list updates as it's retrieved.
    var updatedItemList by remember { mutableStateOf<List<ItemHolder>>(emptyList()) }

    if (updatedItemList.isNotEmpty()) {
        when (tabIndex) {
            0 -> updatedItemList = fullListOfItemHolders
            1 -> updatedItemList = listOfItemHoldersByListId[0]
            2 -> updatedItemList = listOfItemHoldersByListId[1]
            3 -> updatedItemList = listOfItemHoldersByListId[2]
            4-> updatedItemList = listOfItemHoldersByListId[3]
        }
    }

    LaunchedEffect(key1 = Unit) {
        try {
            fullListOfItemHolders = withContext(Dispatchers.IO) {
                getJsonReturnString("https://fetch-hiring.s3.amazonaws.com/hiring.json")
            }
            updatedItemList = fullListOfItemHolders
        } catch (e: Exception) {
            println("Failed to fetch data: ${e.message}")
        }
    }

    Column(modifier = Modifier
        .fillMaxSize()
        .background(colorResource(R.color.grey_300)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center) {
        if (updatedItemList.isEmpty()) {
            CircularProgressIndicator()
        }
        Column {
            ListDisplay(updatedItemList)
        }
    }
}


@Composable
fun ListDisplay(list: List<ItemHolder>) {
    LazyColumn (
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
    ) {
        items (list.size) { index ->
            Column(modifier = Modifier
                .background(colorResource(R.color.grey_200))
                .border(BorderStroke(2.dp, Color.Black)),
            ) {
                Column(modifier = Modifier
                    .fillMaxWidth()
                    ) {
                    CustomTextView("$index", 24, fontWeight = FontWeight.Bold)
                }
                Column(modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 2.dp),
                ) {
                    CustomTextView("listId" + ": " + list[index].listId.toString())
                    CustomTextView("name" + ": " + list[index].name)
                    CustomTextView("id" + ": " + list[index].id.toString())
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

@Composable
fun CustomTextView(text: String, fontSize: Int = 22, fontWeight: FontWeight = FontWeight.Normal) {
    Text(
        modifier = Modifier
            .padding(8.dp),
        fontSize = fontSize.sp,
        fontWeight = fontWeight,
        color = colorResource(R.color.black),
        style = TextStyle(
            fontSize = 24.sp,
            shadow = Shadow(
                color = colorResource(R.color.white), offset = Offset(3.0f, 6.0f), blurRadius = 1f
            )
        ),
        text = text
    )
}

fun getJsonReturnString(url: String): List<ItemHolder> {
    connectToUrl(url)

    var contentReturn = mutableListOf<ItemHolder>()
    jsonReader.beginArray()

    //Single array, multiple objects.
    while (jsonReader.hasNext()) {
        val itemHolder = ItemHolder()
        jsonReader.beginObject()

        while(jsonReader.hasNext()) {
            val key = jsonReader.nextName()

            if (key == "id") {
                itemHolder.id = jsonReader.nextInt()
            } else if (key == "listId") {3
                itemHolder.listId = jsonReader.nextInt()
            } else if (key == "name") {
                if (jsonReader.peek() == JsonToken.NULL) {
                    jsonReader.nextNull()
                    //If null, blank out the name so we can filter out both conditions at once.
                    itemHolder.name = ""
                } else {
                    itemHolder.name = jsonReader.nextString()
            }
            } else {
                jsonReader.skipValue()
            }
        }

        //If name is not blank or null, add data class to its array, otherwise filter it out.
        if (itemHolder.name.isNotEmpty()) {
            contentReturn.add(itemHolder)
        }

        //End of object. This will now loop back up to next object.
        jsonReader.endObject()
    }

    contentReturn = sortedByListIds(contentReturn).toMutableList()
    contentReturn = sortedByNameWithinListIds(contentReturn).toMutableList()

    jsonReader.close()
    myConnection.disconnect()

    return contentReturn
}

fun connectToUrl(url: String) {
    val myUrl = URL(url)
    myConnection = myUrl.openConnection() as HttpsURLConnection
    myConnection.setRequestProperty("id", "object")

    //If our query recipient likes us!
    if (myConnection.responseCode == 200) {
        val response = myConnection.inputStream
        val responseReader = InputStreamReader(response, "UTF-8")

        jsonReader = JsonReader(responseReader)

        Log.i("testConnect", "response code 200 received!")
    } else {
        Log.i("testConnect", "response code 200 NOT received")
    }
}

fun sortedByNameWithinListIds(contentList: MutableList<ItemHolder>): List<ItemHolder> {
    var temporaryContentList = mutableListOf<ItemHolder>()
    val newContentList = mutableListOf<ItemHolder>()
    var previousListId = contentList[0].listId
    val lastObject = contentList.lastOrNull()

    for (currentObject in contentList) {
        //If listID has not changed, add its data class to our temporary data class list.
        if (previousListId == currentObject.listId) {
            temporaryContentList.add(currentObject)
        }
        //If listId has changed OR we're at the end of our loop, it means we're done with the listId group and can sort our temporary list it by name.
        if (previousListId != currentObject.listId || currentObject == lastObject) {
            //Since we're sorting by names within each listId, we call this sort method before re-adding our temporary list to our full one.
            temporaryContentList = itemHolderListSortedByNameNumericValues(temporaryContentList).toMutableList()
            //Making sure we have a new copy of the list to avoid weird references.
            listOfItemHoldersByListId.add(temporaryContentList.toList())
            //Adding sorted temporary list into our full list of the ItemHolder data class, then clearing it for the next listId and adding the first instance of the next listId.
            newContentList.addAll(temporaryContentList)
            temporaryContentList.clear()
            //Adds the next ItemHolder, which now has a new listId, to the next temporary list we're creating.
            temporaryContentList.add(currentObject)
        }

        //Set our previous listId to the one we're iterating through.
        previousListId = currentObject.listId
    }

    return newContentList
}

fun sortedByListIds(contentList: MutableList<ItemHolder>): List<ItemHolder> {
    return contentList.sortedBy { it.listId }
}


fun itemHolderListSortedByNameNumericValues(list: List<ItemHolder>): List<ItemHolder> {
    val nameStringList = mutableListOf<String>()

    for (i in list) {
        //Retaining first part of name String.
        nameStringList.add(i.name.split(" 0")[0])
        //Temporarily setting our name variable to its Int value so we can sort it.
        i.name = i.name.split(" ")[1]
    }

    //Copy of list so we can rejoin its name String below.
    val itemHolderList: List<ItemHolder> = list.sortedBy { it.name.toInt() }

    //Re-adding "Item" String.
    for (i in itemHolderList) {
        i.name = "Item " + i.name
    }

    return itemHolderList
}
