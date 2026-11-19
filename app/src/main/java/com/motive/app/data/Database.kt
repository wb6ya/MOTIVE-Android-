package com.motive.app.data
import androidx.room.*
import kotlinx.coroutines.flow.Flow
@Entity data class Task(@PrimaryKey(autoGenerate=true) val id:Long=0,val title:String,val done:Boolean=false,val created:Long=System.currentTimeMillis())
@Entity data class Video(@PrimaryKey(autoGenerate=true) val id:Long=0,val title:String,val uri:String,val created:Long=System.currentTimeMillis())
@Dao interface MotiveDao {
 @Query("SELECT * FROM Task ORDER BY done ASC, created DESC") fun tasks():Flow<List<Task>>
 @Insert suspend fun addTask(t:Task)
 @Update suspend fun updateTask(t:Task)
 @Delete suspend fun deleteTask(t:Task)
 @Query("SELECT * FROM Video ORDER BY created DESC") fun videos():Flow<List<Video>>
 @Insert suspend fun addVideo(v:Video)
 @Delete suspend fun deleteVideo(v:Video)
}
@Database(entities=[Task::class,Video::class],version=1,exportSchema=false)
abstract class MotiveDb:RoomDatabase(){ abstract fun dao():MotiveDao }