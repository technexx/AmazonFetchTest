package amazon.fetch.test

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import amazon.fetch.test.ui.theme.AmazonFetchTestTheme
import android.content.ClipData.Item
import android.util.JsonReader
import android.util.JsonToken
import android.util.Log
import android.util.Log.i
import androidx.compose.foundation.layout.Column
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.InputStreamReader
import java.net.URL
import javax.net.ssl.HttpsURLConnection

private lateinit var jsonReader : JsonReader
private lateinit var myConnection : HttpsURLConnection

data class ItemHolder (
    var id: Int = 0,
    var listId: Int = 0,
    var name: String = ""
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AmazonFetchTestTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .padding(innerPadding),
                    ) {

                    }
                    runBlocking {
                        launch {
                            //Ui on main thread!

                            //Running on separate thread.
                            withContext(Dispatchers.IO) {
                                connectToUrl("https://fetch-hiring.s3.amazonaws.com/hiring.json")
                            }

                            //Runs one networkPart() finishes
                        }
                    }
                }
            }
        }
    }
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
        getJsonReturnString("")
//        println("return is ${getJsonReturnString("")}")
    } else {
        Log.i("testConnect", "response code 200 NOT received")
    }
}

fun getJsonReturnString(url: String): List<ItemHolder> {
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
    contentReturn = itemHolderListSortedByNameNumericValues(contentReturn).toMutableList()

    jsonReader.close()
    myConnection.disconnect()

    return contentReturn
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
            //Adding sorted temporary list into our full list of the ItemHolder data class, then clearing it for the next listId and adding the first instance of the next listId.
            newContentList.addAll(temporaryContentList)
            temporaryContentList.clear()
            temporaryContentList.add(currentObject)
        }

        //Set our previous listId to the one we're iterating through.
        previousListId = currentObject.listId
    }

    for (i in newContentList) {
        println("$i")
    }

    return newContentList
}

fun sortedByListIds(contentList: MutableList<ItemHolder>): List<ItemHolder> {
    return contentList.sortedBy { it.listId }
}

fun itemHolderListSortedByNameNumericValues(itemHolderList: List<ItemHolder>): List<ItemHolder> {
    val stringArray = ArrayList<String>()
    val numberArray = ArrayList<Int>()

    for (i in itemHolderList) {
        val splitName = i.name.split(" ")
        stringArray.add(splitName[0])
        numberArray.add(splitName[1].toInt())

        i.name = splitName[1]
    }

    return itemHolderList.sortedBy { it.name.toInt() }
}