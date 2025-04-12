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
import android.util.JsonReader
import android.util.Log
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

//            assignJsonReturnString("feeds_url")

        Log.i("testConnect", "response code 200 received!")
        Log.i("return", "$jsonReader")
    } else {
        Log.i("testConnect", "response code 200 NOT received")
    }
}

fun getJsonReturnString(key: String): String {
    var jsonReturnString: String = ""
    jsonReader.beginObject()

    while (jsonReader.hasNext()) {
//        val value = jsonReader.nextName()
//        if (key == value) {
//            jsonReturnString = jsonReader.nextString()
//            break
//        } else jsonReader.skipValue()
    }
    return jsonReturnString

//    jsonReader.close()
//    myConnection.disconnect()
}