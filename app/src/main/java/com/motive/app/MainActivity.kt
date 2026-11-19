package com.motive.app
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.room.Room
import com.motive.app.data.*
import com.motive.app.ui.theme.MotiveTheme
import kotlinx.coroutines.launch

class MainActivity:ComponentActivity(){
 private lateinit var dao:MotiveDao
 private val picker=registerForActivityResult(ActivityResultContracts.OpenDocument()){ u:Uri? ->
  u?.let { uri -> try{contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)}catch(_:Exception){}
   lifecycleScope.launch { dao.addVideo(Video(title=uri.lastPathSegment?.substringAfterLast('/')?.substringAfterLast(':') ?: "Video",uri=uri.toString())) }
  }
 }
 override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState)
  dao=Room.databaseBuilder(applicationContext,MotiveDb::class.java,"motive.db").build().dao()
  setContent { MotiveTheme { App(dao){picker.launch(arrayOf("video/*"))} } }
 }
}
private enum class Tab{HOME,TASKS,LIBRARY,STATS,SETTINGS}
@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun App(dao:MotiveDao,pick:()->Unit){
 var tab by remember{mutableStateOf(Tab.HOME)}; var ar by remember{mutableStateOf(true)}
 val tasks by dao.tasks().collectAsState(emptyList()); val videos by dao.videos().collectAsState(emptyList())
 val scope= rememberCoroutineScope()
 CompositionLocalProvider(LocalLayoutDirection provides if(ar) LayoutDirection.Rtl else LayoutDirection.Ltr){
 Scaffold(containerColor=MaterialTheme.colorScheme.background,topBar={TopAppBar(title={Text("MOTIVE",fontWeight=FontWeight.Black,color=MaterialTheme.colorScheme.primary)},actions={TextButton(onClick={ar=!ar}){Text(if(ar)"EN" else "عربي")}},colors=TopAppBarDefaults.topAppBarColors(containerColor=MaterialTheme.colorScheme.background))},
 bottomBar={NavigationBar { listOf(Triple(Tab.HOME,if(ar)"الرئيسية" else "Home",Icons.Outlined.Home),Triple(Tab.TASKS,if(ar)"المهام" else "Tasks",Icons.Outlined.CheckCircle),Triple(Tab.LIBRARY,if(ar)"المكتبة" else "Library",Icons.Outlined.VideoLibrary),Triple(Tab.STATS,if(ar)"الإحصائيات" else "Stats",Icons.Outlined.BarChart),Triple(Tab.SETTINGS,if(ar)"الإعدادات" else "Settings",Icons.Outlined.Settings)).forEach{(t,l,i)->NavigationBarItem(selected=tab==t,onClick={tab=t},icon={Icon(i,null)},label={Text(l,maxLines=1)})} } }
 ){ pad ->
 when(tab){
 Tab.HOME -> Page(pad){ Header(if(ar)"مساحة إنجازك" else "Your focus space",if(ar)"خطوة صغيرة اليوم تصنع فرقاً." else "Small steps make progress.")
  CardBox { Text(if(ar)"إنجاز المهام" else "Task progress"); val n=tasks.count{it.done}; Text("$n / ${tasks.size}",style=MaterialTheme.typography.headlineLarge); LinearProgressIndicator(progress={if(tasks.isEmpty())0f else n.toFloat()/tasks.size},modifier=Modifier.fillMaxWidth()) }
  Button(onClick={tab=Tab.TASKS}){Text(if(ar)"إدارة المهام" else "Manage tasks")}
  tasks.take(4).forEach{Text((if(it.done)"✓ " else "• ")+it.title)}
 }
 Tab.TASKS -> { var title by remember{mutableStateOf("")}
  Page(pad){Header(if(ar)"المهام" else "Tasks",if(ar)"حوّل أهدافك إلى خطوات." else "Turn goals into steps.")
   CardBox { OutlinedTextField(title,{title=it},modifier=Modifier.fillMaxWidth(),label={Text(if(ar)"اسم المهمة" else "Task title")},singleLine=true)
    Button(onClick={if(title.isNotBlank()){scope.launch{dao.addTask(Task(title=title.trim()))};title=""}},modifier=Modifier.fillMaxWidth()){Text(if(ar)"إضافة مهمة" else "Add task")} }
   if(tasks.isEmpty()) Text(if(ar)"لا توجد مهام بعد." else "No tasks yet.")
   tasks.forEach{t->CardBox{Row(verticalAlignment=Alignment.CenterVertically){Checkbox(t.done,{v->scope.launch{dao.updateTask(t.copy(done=v))}});Text(t.title,Modifier.weight(1f));IconButton(onClick={scope.launch{dao.deleteTask(t)}}){Icon(Icons.Outlined.Delete,null)}}}}
  }
 }
 Tab.LIBRARY -> { var selected by remember{mutableStateOf<Video?>(null)}
  Page(pad){Header(if(ar)"مكتبتك" else "Your library",if(ar)"محتوى التعلم في مكان واحد." else "Your learning videos in one place.")
   Button(onClick=pick,modifier=Modifier.fillMaxWidth()){Icon(Icons.Outlined.Add,null);Text(if(ar)"استيراد فيديو" else "Import video")}
   if(videos.isEmpty()) Text(if(ar)"المكتبة فارغة." else "Library is empty.")
videos.forEach{v->CardBox{Row(verticalAlignment=Alignment.CenterVertically){Text(v.title,Modifier.weight(1f));IconButton(onClick={selected=v}){Icon(Icons.Outlined.PlayArrow,null)};IconButton(onClick={scope.launch{dao.deleteVideo(v)}}){Icon(Icons.Outlined.Delete,null)}}}}  }
  selected?.let{v->PlayerDialog(v){selected=null}}
 }
 Tab.STATS -> Page(pad){Header(if(ar)"الإحصائيات" else "Statistics",if(ar)"تابع تقدمك." else "Track your progress.")
  val done=tasks.count{it.done}; val pct=if(tasks.isEmpty())0 else done*100/tasks.size
  listOf((if(ar)"إجمالي المهام" else "Total tasks") to tasks.size,(if(ar)"المهام المكتملة" else "Completed") to done,(if(ar)"معدل الإنجاز" else "Completion") to pct,(if(ar)"الفيديوهات" else "Videos") to videos.size).forEach{(l,n)->CardBox{Text(l);Text(if(l.contains("معدل")||l=="Completion")"$n%" else "$n",style=MaterialTheme.typography.headlineLarge)}}
 }
 Tab.SETTINGS -> Page(pad){Header(if(ar)"الإعدادات" else "Settings",if(ar)"خصص تجربتك." else "Customize your experience.")
  CardBox{Text(if(ar)"اللغة" else "Language");Button(onClick={ar=!ar}){Text(if(ar)"التبديل إلى English" else "Switch to Arabic")}}
  CardBox{Text("MOTIVE 1.0.0 MVP");Text(if(ar)"البيانات محفوظة محلياً على جهازك." else "Data is stored locally on your device.")}
 }
 }
 }
 }
}
@Composable private fun Page(padding:PaddingValues,content:@Composable ColumnScope.()->Unit){Column(Modifier.fillMaxSize().padding(padding).padding(18.dp).verticalScroll(androidx.compose.foundation.rememberScrollState()),verticalArrangement=Arrangement.spacedBy(14.dp),content=content)}
@Composable private fun Header(title:String,sub:String){Column{Text(title,style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold);Text(sub,color=MaterialTheme.colorScheme.onSurfaceVariant)}}
@Composable private fun CardBox(content:@Composable ColumnScope.()->Unit){Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(20.dp),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface)){Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp),content=content)}}
@Composable private fun PlayerDialog(video:Video,close:()->Unit){
 val ctx=androidx.compose.ui.platform.LocalContext.current
 val player=remember(video.uri){ExoPlayer.Builder(ctx).build().apply{setMediaItem(MediaItem.fromUri(Uri.parse(video.uri)));prepare();playWhenReady=true}}
 DisposableEffect(player){onDispose{player.release()}}
 Dialog(onDismissRequest=close){Card{Column(Modifier.padding(12.dp)){Text(video.title);AndroidView(factory={PlayerView(it).apply{this.player=player}},modifier=Modifier.fillMaxWidth().height(220.dp));TextButton(onClick=close){Text("Close")}}}}
}
