import scala.xml._
import scala.concurrent.future
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.ExecutionContext
import java.util.concurrent.Executors
import java.io.FileWriter
import sys.process._

import scala.io.Source
import scala.concurrent.duration._
import scala.concurrent._
import spray.json._
//import DefaultJsonProtocol._

object ytdt extends App {
	case class Subscription(channelName: String, channelId: String)
	case class Config(dlLoc: String, prevDldLoc: String, subList: List[Subscription])
	case class VideoLink(name: String, url: String)

	object MyJsonProt extends DefaultJsonProtocol {
		implicit val subJsonConverter = jsonFormat(Subscription, "channelName", "channelId")
		implicit val configJsonConverter = jsonFormat(Config, "videoDownloadLocation", "alreadyDownloadedListLocation",
				"subscriptions")
	}
	import MyJsonProt._
	

	//implicit val global = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(2))

	def loadConfig(configFileLoc: String): Config = {
		val configFileString = Source.fromFile(configFileLoc).mkString
		val jsonObj = configFileString.parseJson
		
		jsonObj.convertTo[Config]
	}

	def getVideoList(sub: Subscription)(implicit conf: Config): Future[Seq[VideoLink]] = {
		val videoListFut = future {
			val oldUrls = getUrlsFromFile(conf.prevDldLoc + "/" + sub.channelName + ".list")
			val feed = XML.load("https://www.youtube.com/feeds/videos.xml?channel_id=" + sub.channelId)
			val urlList = (feed \ "entry" \\ "@href").map( _ text).reverse
			val titleList = (feed \ "entry" \ "title").map( _ text).reverse
			val tmp = urlList zip titleList
			tmp.filter( tupA => ! oldUrls.contains(tupA._1))
				.map( tupB => VideoLink(sub.channelName, tupB._1))
		}
		videoListFut
	}

	def getUrlsFromFile(fileName: String) = {
		try {
			Source.fromFile(fileName).getLines.toList
		}
		catch {
			case ex : Exception => println(ex); List[String]()
		}
	}

	def downloadVideo(link: VideoLink)(implicit conf: Config): Boolean = {
		val cmdStr = "youtube-dl -x --audio-format vorbis --restrict-filenames" +
				" -o " + conf.dlLoc + link.name + "/%(uploader)s_%(upload_date)s_%(title)s.%(ext)s " +
				link.url
		println(cmdStr)
		if( (cmdStr !) == 0 ){
		//add file to already downloaded
		val fw = new FileWriter(conf.prevDldLoc + "/" + link.name + ".list", true)
		try{
			fw.write(link.url + "\n")
		}
		finally{
			fw.close()
		}
	}
	true
	}

implicit val conf = loadConfig(System.getProperty("user.home") + "/.yt-dl/yt-dl.conf")

val fUrls: Future[List[Seq[VideoLink]]] = Future.sequence(conf.subList.map( getVideoList(_) ))

val fVideos: Future[List[Boolean]] = fUrls.flatMap(
{	ls: List[Seq[VideoLink]] => Future.sequence(
	ls.flatMap({ svl: Seq[VideoLink] =>
		svl.map({ vl: VideoLink =>  
			future{
				println(Thread.currentThread().getId())
				downloadVideo(vl)
			}	
		})
	}))
})

Await.result(fVideos, Duration.Inf)


println("Done");

}




