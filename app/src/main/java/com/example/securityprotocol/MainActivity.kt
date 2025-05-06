package com.example.securityprotocol

/*
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
import com.example.securityprotocol.ui.theme.SecurityProtocolTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SecurityProtocolTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        name = "Android",
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    SecurityProtocolTheme {
        Greeting("Android")
    }
}
 */


import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.securityprotocol.ux.SingleMessaging
import com.example.securityprotocol.ux.SingleMessagingAlice
import com.example.securityprotocol.ux.SingleMessagingBob

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.singleChat).setOnClickListener {
            startActivity(Intent(
                this@MainActivity,
                SingleMessaging::class.java
            ))
        }

        findViewById<Button>(R.id.singleChat).setOnClickListener {
            startActivity(Intent(
                this@MainActivity,
                SingleMessagingAlice::class.java
            ))
        }

        findViewById<Button>(R.id.singleChat).setOnClickListener {
            startActivity(Intent(
                this@MainActivity,
                SingleMessagingBob::class.java
            ))
        }

//        findViewById<Button>(R.id.groupChat).setOnClickListener {
//            startActivity(Intent(
//                this@MainActivity,
//                GroupMessaging::class.java
//            ))
//        }
    }
}