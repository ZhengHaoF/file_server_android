package com.example.myapplication

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebSettings.LOAD_DEFAULT
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import com.example.myapplication.ui.theme.MyApplicationTheme
import com.example.myapplication.ui.theme.TextColor2
import com.google.gson.Gson
import io.sanghun.compose.video.VideoPlayer
import io.sanghun.compose.video.uri.VideoPlayerMediaItem
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import kotlin.math.roundToInt


var FileItemList: SnapshotStateList<FileItem> = SnapshotStateList()
//var serverPath = mutableStateOf("http://192.168.8.185:3000")

var serverPath = mutableStateOf("https://file.zhfblog.top")
var path = mutableStateOf("$")
val loadingView = mutableStateOf(true)
val openDialog = mutableStateOf(false)
val openVideoDialog = mutableStateOf(false)
val openSettingDialog = mutableStateOf(false)
val showFileUrl = mutableStateOf("")
val showFileIndex = mutableStateOf(0)
val mode = mutableStateOf("") //WebView
val onlyImg =  mutableStateOf(false)
val dirInFirst =  mutableStateOf(true)
val fileNameSorting =  mutableStateOf(false)
class MainActivity : ComponentActivity() {
    private var doubleBackToExitPressedOnce = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        getListData()
        setContent {
            MyApplicationTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    color = MaterialTheme.colorScheme.background
                ) {

                    if (mode.value === "WebView") {
                        ComposableWebView(serverPath.value)
                    } else {
                        ScaffoldExample()
                    }
                    if (openDialog.value) {
                        ImageViewDialog(openDialog, showFileUrl.value)
                    }
                    if (openVideoDialog.value) {
                        VideoExampleScreen(openVideoDialog, showFileUrl.value)
                    }
                    if (openSettingDialog.value) {
                        SettingDialog(openSettingDialog,applicationContext)
                    }
                }

            }
        }
        onBackPressedDispatcher.addCallback() {

            if (openDialog.value || openVideoDialog.value) {
                openDialog.value = false
                openVideoDialog.value = false
            } else {
                if (path.value == "$") {
                    if (doubleBackToExitPressedOnce) {
                        // 第二次按返回键，退出应用
                        finishAffinity() // 如果需要，可以加上System.exit(0)，但通常不推荐
                        return@addCallback // 表示已处理
                    }
                    doubleBackToExitPressedOnce = true
                    Toast.makeText(applicationContext, "再按一次退出应用", Toast.LENGTH_SHORT).show()


                    // 创建一个 Handler，并将其与当前线程的 Looper 关联（在主线程中通常是 Looper.getMainLooper()）
                    val handler = Handler(Looper.getMainLooper())
                    // 定义一个 Runnable，它包含你想要延迟执行的操作
                    val runnable = Runnable {
                        // 在这里执行你的操作
                        doubleBackToExitPressedOnce = false
                    }

                    // 使用 postDelayed 方法安排 Runnable 在 1000 毫秒（1 秒）后执行
                    handler.postDelayed(runnable, 2000)
                } else {
                    prePath()
                }
            }

        }
    }


}

// 定义一个内部类来表示列表中的单个项
data class FileItem(
    val name: String,
    val size: Long,
    val isDirectory: Boolean,
    val isFile: Boolean,
    val suffix: String,
    val mtime: String // 假设我们使用ZonedDateTime来处理时间戳
)

// 定义一个外部类来表示整个JSON结构
data class FileList(
    val listNum: Int,
    var list: List<FileItem>
)

@Composable
fun ItemListView(
    openDialog: MutableState<Boolean>,
    openVideoDialog: MutableState<Boolean>,
    showFileUrl: MutableState<String>,
    listHeight: Dp
) {
    if (FileItemList.size > 0) {
        LazyColumn(
            Modifier
                .height(listHeight)
                .padding(bottom = 5.dp),
        ) {
            itemsIndexed(
                FileItemList
            ) { index, item ->
                Card(
                    modifier = Modifier
                        .padding(start = 16.dp, end = 16.dp, top = 10.dp)
                        .clickable {
                            if (isImage(item.suffix)) {
                                showFileUrl.value = getFileUrl(item.name)
                                showFileIndex.value = index
                                openDialog.value = true
                            } else if (item.isDirectory) {
                                path.value += "__${item.name}"
                                getListData()
                            } else if (isVideo(item.suffix)) {
                                showFileUrl.value = getFileUrl(item.name)
                                showFileIndex.value = index
                                openVideoDialog.value = true
                            }
                        }
                ) {
                    Column(
                        Modifier
                            .padding(16.dp)
                            .fillMaxWidth()
                    ) {
                        Row {
                            if (isImage(item.suffix)) {
                                Surface(
                                    shape = CircleShape
                                ) {
                                    SubcomposeAsyncImage(
                                        model = "${getFileUrl(item.name)}!250x250",
                                        contentDescription = null,
                                        modifier = Modifier.size(50.dp),
                                        contentScale = ContentScale.Crop
                                    ) {
                                        val state = painter.state
                                        if (state is AsyncImagePainter.State.Loading || state is AsyncImagePainter.State.Error) {
                                            CircularProgressIndicator()
                                        } else {
                                            SubcomposeAsyncImageContent()
                                        }
                                    }
                                }
                            }

                            Column(
                                Modifier.padding(start = 10.dp)
                            ) {
                                Text(
                                    fontSize = 20.sp,
                                    text = item.name,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (item.isFile) {
                                    Text(
                                        color = TextColor2,
                                        fontSize = 16.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        text = "${(item.size / 1024 / 1024.0).roundToInt()}MB"
                                    )
                                } else {
                                    Text(
                                        color = TextColor2,
                                        fontSize = 16.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        text = "文件夹"
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    } else {
//        Box(
//            modifier = Modifier
//                .fillMaxWidth()
//                .height(listHeight)
//        ) {
//            Text(
//                text = "没有文件", modifier = Modifier.align(Alignment.Center),
//                style = TextStyle(
//                    fontSize = 30.sp,
//                    letterSpacing = 7.sp
//                )
//            )
//        }
    }
}


@SuppressLint("SetJavaScriptEnabled", "JavascriptInterface")
@Composable
fun ComposableWebView(
    url: String,
    modifier: Modifier = Modifier,
    onWebViewReady: (WebView) -> Unit = {}
) {
    val context = LocalContext.current

    Box(modifier) {
        val webView = remember { WebView(context) }
        webView.settings.javaScriptEnabled = true
        webView.settings.cacheMode = LOAD_DEFAULT


        // 添加 JavaScript 接口
        webView.addJavascriptInterface(
            WebAppInterface(context),
            "Android"
        )

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)

                // WebView 加载完成后的回调
                onWebViewReady(webView)
            }
        }

        webView.loadUrl(url)
        webView.evaluateJavascript("javascript: document.body.style.backgroundColor = 'red'", null)
        // 将 WebView 嵌入到 Compose 中
        AndroidView({ webView }, modifier = Modifier.fillMaxSize())
    }
}


// JavaScript 接口类
class WebAppInterface(private val context: Context) {

    @JavascriptInterface
    fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    @JavascriptInterface
    fun playVideo(url: String) {
        Toast.makeText(context, url, Toast.LENGTH_SHORT).show()
        showFileUrl.value = url
        openVideoDialog.value = true
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScaffoldExample() {
    Scaffold(
        topBar = {
            TopAppBar(
                colors = topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                ),
                title = {
                    Text("简易文件服务器 1.0")
                },
                navigationIcon = {
                    IconButton(onClick = {
                        prePath()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = null
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        openSettingDialog.value = true
                    }) {
                        Icon(Icons.Default.Settings, contentDescription = "Setting")
                    }
                }
            )
        },
//        bottomBar = {
//            BottomAppBar(
//                modifier = Modifier.height(60.dp),
//                containerColor = MaterialTheme.colorScheme.primaryContainer,
//                contentColor = MaterialTheme.colorScheme.primary,
//            ) {
//                Button(
//                    onClick = {
//                        prePath()
//                    },
//                    modifier = Modifier
//                        .fillMaxSize(),
//                    shape = CutCornerShape(0.dp)
//                ) {
//                    Text(text = "返回")
//                }
//            }
//        },
//        floatingActionButton = {
//            FloatingActionButton(onClick = { }) {
//                Icon(Icons.Default.Settings, contentDescription = "Add")
//            }
//        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                if (loadingView.value) {
                    LinearProgressIndicator(
                        Modifier.align(Alignment.Center)
                    )
                } else {
                    BoxWithConstraints(
                        Modifier.fillMaxSize()
                    ) {
                        val listHeight =
                            pxToDp(LocalContext.current, (constraints.maxHeight)).dp
                        Column {
                            ItemListView(
                                openDialog,
                                openVideoDialog,
                                showFileUrl,
                                listHeight
                            )
                        }
                    }
                }


            }
        }
    }
}


@Composable
fun VideoExampleScreen(openVideoDialog: MutableState<Boolean>, url: String) {
    if (openVideoDialog.value && url.isNotEmpty()) {
        VideoPlayer(
            mediaItems = listOf(
                VideoPlayerMediaItem.NetworkMediaItem(
                    url = url,
                )
            ),
            enablePip = false,
            modifier = Modifier
                .fillMaxSize()
        )
    }
}

/**
 * 图片预览弹窗
 */
@Composable
fun ImageViewDialog(openDialog: MutableState<Boolean>, imgUrl: String = "") {
    var offset by remember { mutableStateOf(Offset.Zero) }
    var scale by remember { mutableFloatStateOf(1f) }
    offset = Offset.Zero
    scale = 1f
    if (openDialog.value && imgUrl.isNotEmpty()) {
        Dialog(
            onDismissRequest = { openDialog.value = false },
            properties = DialogProperties(usePlatformDefaultWidth = false) //全屏
        ) {
            Box(
                Modifier
                    .fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                SubcomposeAsyncImage(
                    model = imgUrl,
                    contentDescription = "加载中"
                ) {
                    val state = painter.state
                    if (state is AsyncImagePainter.State.Loading || state is AsyncImagePainter.State.Error) {
                        LinearProgressIndicator(

                        )
                    } else {
                        SubcomposeAsyncImageContent(
                            modifier = Modifier
                                .scale(scale)
                                .offset {
                                    IntOffset(offset.x.roundToInt(), offset.y.roundToInt())
                                }
                                .pointerInput(Unit) {
                                    detectTransformGestures(
                                        panZoomLock = false, // 平移或放大时是否可以旋转
                                        onGesture = { centroid: Offset, pan: Offset, zoom: Float, rotation: Float ->
                                            offset += pan
                                            scale *= zoom
                                        }
                                    )
                                },
                        )
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween, // 使用 SpaceBetween 来在子项之间分配空间
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                //上一张图
                                IconButton(
                                    onClick = {
                                        var index = FileItemList.size - 1
                                        for (item in FileItemList.asReversed()) {
                                            if (index < showFileIndex.value && isImage(item.suffix)) {
                                                showFileUrl.value = getFileUrl(item.name)
                                                showFileIndex.value = index
                                                break
                                            }
                                            index--
                                        }

                                    },
                                    modifier = Modifier
                                        .weight(1f, fill = false)
                                        .background(
                                            Color.White.copy(alpha = 0.2f),
                                            shape = CircleShape
                                        )
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                        contentDescription = null
                                    )
                                }
                                // Spacer 用于占据中间的空间
                                Spacer(modifier = Modifier.weight(1f))
                                //下一张图
                                IconButton(
                                    onClick = {
                                        var index = 0
                                        for (item in FileItemList) {
                                            if (index > showFileIndex.value && isImage(item.suffix)) {
                                                showFileUrl.value = getFileUrl(item.name)
                                                showFileIndex.value = index
                                                break
                                            }
                                            index++
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1f, fill = false)
                                        .background(
                                            Color.White.copy(alpha = 0.2f),
                                            shape = CircleShape
                                        )
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        contentDescription = null
                                    )
                                }
                            }
                        }
                    }
                }

            }
        }
    }
}

/**
 * 设置弹窗
 */
@Composable
fun SettingDialog(openSettingDialog: MutableState<Boolean>,context: Context) {
    if (openSettingDialog.value) {
        Dialog(
            onDismissRequest = { openSettingDialog.value = false },
        ) {
            Card(
                Modifier
                    .fillMaxWidth()
                    .height(500.dp)
            ) {
                Column(
                    Modifier.padding(start = 16.dp, end = 16.dp),
                ) {
                    var textValue by remember { mutableStateOf( serverPath.value) }
                    Column {
                        TextField(
                            value = textValue,
                            onValueChange = {
                                textValue = it
                            },
                            label = { Text("服务器地址") },

                            )
                    }
                    Column {
                        Row{
                            Text(text = "文件名倒序",Modifier.padding(top = 15.dp))
                            Spacer(modifier = Modifier.weight(1f))
                            Checkbox(checked = fileNameSorting.value, onCheckedChange = {
                                fileNameSorting.value = it
                            })
                        }
                        Row{
                            Text(text = "是否只显示图片和视屏",Modifier.padding(top = 15.dp))
                            Spacer(modifier = Modifier.weight(1f))
                            Checkbox(checked = onlyImg.value, onCheckedChange = {
                                onlyImg.value = it
                            })
                        }
                        Row{
                            Text(text = "文件夹在最上方",Modifier.padding(top = 15.dp))
                            Spacer(modifier = Modifier.weight(1f))
                            Checkbox(checked = dirInFirst.value, onCheckedChange = {
                                dirInFirst.value = it
                            })
                        }
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Column {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween, // 使用 SpaceBetween 来在子项之间分配空间
                        ) {
                            Button(
                                onClick = {
                                    openSettingDialog.value = false
                                },
                                colors = ButtonDefaults.buttonColors(Color.LightGray)
                            ) {
                                Text(text = "取消",color = Color.Black)
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            Button(onClick = {
                                if(textValue.isEmpty()){
                                    Toast.makeText(context, "服务器地址不能为空", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                serverPath.value = textValue
                                openSettingDialog.value = false
                                path.value = "$"
                                getListData()
                            }) {
                                Text(text = "确定")
                            }
                        }
                    }
                }
            }
        }
    }

}

fun prePath() {
    val lastIndexOfDoubleUnderscore = path.value.lastIndexOf("__")
    if (lastIndexOfDoubleUnderscore != -1) {
        path.value = path.value.substring(0, lastIndexOfDoubleUnderscore)
    }
    getListData()
}

fun pxToDp(context: Context, px: Float): Int {
    val density = context.resources.displayMetrics.density
    return (px / density + 0.5f).toInt()
}

fun pxToDp(context: Context, px: Int): Int {
    // 由于Int不能直接除以Float，我们需要先将Int转换为Float
    return pxToDp(context, px.toFloat())
}

/**
 * 判断是否是图片文件
 */
fun isImage(suffix: String): Boolean {
    val upperSuffix = suffix.uppercase()
    val imageList = listOf(".JPG", ".PNG", ".WEBP")
    return imageList.contains(upperSuffix)
}

/**
 * 判断是否是视屏文件
 */
fun isVideo(suffix: String): Boolean {
    val upperSuffix = suffix.uppercase()
    val imageList = listOf(".MP4", ".AVI", ".FLV", ".RMVB", "MOV", ".MKV", ".WMV")
    return imageList.contains(upperSuffix)
}


/**
 * 获取文件列表
 */
fun getFileUrl(fileName: String): String {
    val newPath = path.value.replace("__", "/").replace("$", "")
    return urlFormatting("${serverPath.value}/getFile/${newPath}/${fileName}")
}

/**
 * 图片的下载地址有些有双斜杠，有些没有，统一给它格式化一下
 * @param url 链接
 * @return 格式化后的URL
 */
fun urlFormatting(url: String): String {
    val protocolEndIndex = url.indexOf("://")
    if (protocolEndIndex == -1) {
        // 如果不是有效的URL（没有协议），则返回原URL
        return url
    }

    val protocol = url.substring(0, protocolEndIndex)
    val rest = url.substring(protocolEndIndex + 3) // "://"长度为3

    // 使用正则表达式替换两个或更多连续斜杠为一个斜杠
    val formattedRest = rest.replace("/{2,}".toRegex(), "/")

    return "$protocol://$formattedRest"
}

//@Composable
//fun GetDateBtn() {
//    Box(modifier = Modifier.fillMaxWidth()) {
//        Column(
//            modifier = Modifier.fillMaxWidth()
//        ) {
//            Button(
//                onClick = {
//                    getListData()
//                },
//                modifier = Modifier.fillMaxWidth(),
//                shape = CutCornerShape(0.dp)
//            ) {
//                Text("测试")
//            }
//        }
//    }
//
//}

/**
 * 获取文件列表
 */
fun getListData() {

    loadingView.value = true
//    println("Path:${rootPath.value}/list/${path.value}")
    FileItemList.clear()
    sendGetRequest("${serverPath.value}/list/${path.value}") { response, error ->
        loadingView.value = false
        if (error != null) {
            // 处理错误
            println("请求错误: $error")
        } else {
            // 处理响应
            val gson = Gson()
            val personFromJson = gson.fromJson(response, FileList::class.java)
            var newList: List<FileItem> = personFromJson.list

            if(!fileNameSorting.value){
                newList = newList.sortedBy { it.name }
            }else{
                newList = newList.sortedByDescending { it.name }
            }
            if(!dirInFirst.value){
                newList = newList.sortedBy { it.isDirectory }
            }else if(dirInFirst.value){
                newList = newList.sortedByDescending  { it.isDirectory }
            }
            if(onlyImg.value){
                newList =  newList.filter { isImage(it.suffix) || it.isDirectory || isVideo(it.suffix) }
            }

            newList.forEach {
                FileItemList.add(it)
            }
            println("Response: $response")
        }
    }
}


fun sendGetRequest(url: String, callback: (String?, Throwable?) -> Unit) {
    val client = OkHttpClient()

    val request = Request.Builder()
        .url(url)
        .build()

    client.newCall(request).enqueue(object : okhttp3.Callback {
        override fun onFailure(call: okhttp3.Call, e: IOException) {
            e.printStackTrace()
            callback(null, e)
        }

        override fun onResponse(call: okhttp3.Call, response: Response) {
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                callback(responseBody, null)
            } else {
                callback(null, Exception("Unexpected code $response"))
            }
        }
    })
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MyApplicationTheme {
//        ItemListView()
    }
}