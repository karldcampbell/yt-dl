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

object ytdt extends App {

	case class VideoLink(name: String, url: String)
	//implicit val global = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(2))

	def getVideoList(channelId: String, name: String) = {
		val videoListFut = future {
			val oldUrls = getUrlsFromFile(name + ".list")
			val feed = XML.load("https://www.youtube.com/feeds/videos.xml?channel_id=" + channelId)
			val urlList = (feed \ "entry" \\ "@href").map( _ text).reverse
			val titleList = (feed \ "entry" \ "title").map( _ text).reverse
			val tmp = urlList zip titleList
			tmp.filter( tupA => ! oldUrls.contains(tupA._1))
				.map( tupB => VideoLink(name, tupB._1))
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

	def downloadVideo(link: VideoLink): Boolean = {
		val cmdStr = "youtube-dl -x --audio-format vorbis --restrict-filenames" +
				" -o ~/Downloads/youbube/%(uploader)s_%(upload_date)s_%(title)s.%(ext)s " +
				link.url
		println(cmdStr)
		if( (cmdStr !) == 0 ){
		//add file to already downloaded
		val fw = new FileWriter(link.name + ".list", true)
		try{
			fw.write(link.url + "\n")
		}
		fw.close()
	}
	true
	}
val urls = List(getVideoList("UC7iaZh8Nk5i-5NuMOhk6jKA", "JanetBloomfield"),
				getVideoList("UCcdQla_9PbQb4-FKFxJN2ag", "DrwarrenFarrell"),
				getVideoList("UC6TJdRrZR_WacbxJWiRZ5_g", "DavisAurini"),
				getVideoList("UCmkSQppUOY6r7qd-sbcftBQ", "TheCriticalG"),
				getVideoList("UC-yewGHQbNFpDrGM0diZOLA", "SargonOfAkkad"),
				getVideoList("UCr3qf3JVwW_41j4LUQZtu9Q", "BernardChapin"),
				getVideoList("UCcmnLu5cGUGeLy744WS-fsg", "KarenStraughan"),
				getVideoList("UCeFlOi54kYIgbJHt_1ApDpg", "TheJusticar"),
				getVideoList("UCzOnXvfwc32YEiwTe_8Nf-g", "RockingMrE"),
				getVideoList("UCtD9a-aXIYS6-e-8s7DISiw", "HowTheWorldWorks"),
				getVideoList("UCy9VHF_ihqzBsx9tr0_XZIQ", "TruthRevoltOriginals"))

val videos = urls map {
		_ map { list =>
					list map { vl =>
						future {
						downloadVideo(vl)
				}		
			}
		}
	}

Await.result(Future.sequence(videos), Duration.Inf)

println("Done");

}




