import org.apache.spark.streaming.{Seconds, StreamingContext}
import org.apache.spark.streaming.twitter._
import org.apache.spark.SparkConf
import org.apache.log4j.{Level, Logger}


object SentimentAnalysis {
  
  def main(args: Array[String]) {
    Logger.getLogger("org").setLevel(Level.ERROR)


    // Get Twitter Token and the filter words
    // By default, use my token and filter word is Trump
    var consumerKey ="SweK0ImF4kqSk26cEHZ2t4C47"
    var consumerSecret = "eA5vufQY7Kr24wxziRLQV8LVTjY61X1J0REA90ULfkGOaE1ema"
    var accessToken = "79481024-t0FHjhajmtNZPZoatkfX8CuUOBCEgD8UDL5pzvRN7"
    var accessTokenSecret = "LX9NSy1MzPLl1HvAEztvhBaQvSo46TqXdpvygjIyN4jbT"
    var time = 0;

  if (args.length > 3) {
      // get data from your setting
    val  Array(consumerKey, consumerSecret, accessToken, accessTokenSecret) = args.take(4)
      time = toInt(args.takeRight(args.length - 4))
      if (time==0) {
          time = 120;
      }
    }

    // Set the system properties so that Twitter4j library used by twitter stream
    // can use them to generat OAuth credentials
    System.setProperty("twitter4j.oauth.consumerKey", consumerKey)
    System.setProperty("twitter4j.oauth.consumerSecret", consumerSecret)
    System.setProperty("twitter4j.oauth.accessToken", accessToken)
    System.setProperty("twitter4j.oauth.accessTokenSecret", accessTokenSecret)
      
    print(consumerKey)

    // Set twitter stream
    val sparkConf = new SparkConf().setAppName("Twitter").setMaster("local[2]")
    val ssc = new StreamingContext(sparkConf, Seconds(5))
    val stream = TwitterUtils.createStream(ssc, None)      //filters was inside the stream

    val tags = stream.flatMap { status => status.getHashtagEntities.map(_.getText)}
      
    val topCount = tags.map((_, 1)).reduceByKeyAndWindow((a:Int,b:Int) => (a + b), Seconds(time),Seconds(time)).map{case (topic, count) => (count, topic)}.transform(_.sortByKey(false))
      
    // Print popular hashtags
    topCount.foreachRDD(rdd => {
    val topList = rdd.take(10)
    println(s"\nPopular topics in last $time seconds (%s total):".format(rdd.count()))
    topList.foreach{case (count, tag) => println("%s (%s tweets)".format(tag, count))}
    })
      
    topCount.saveAsTextFiles("output/populars/tags")

    ssc.start()
    ssc.awaitTermination()

  }
    
    def toInt(s: Unit): Int = {
      try {
        s.toString.toInt
      } catch {
        case e: Exception => 0
      }
}

}

