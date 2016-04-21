import groovy.json.JsonSlurper

//def minutes_of_SDT = 60
def my_displayname = "EdgeRouter"
def company = "suding"
def api_user = "api4"
def api_pass = "api.805"

def qty_of_pings_per_batch = 3
def qty_of_batches = 2
def seconds_between_batches = 1

def threshold_average_ms = 77
def threshold_ping_loss_percent = 70


def displayname = URLEncoder.encode(my_displayname, "UTF-8")
String hostInfo = HTTP.body("https://" + company + ".logicmonitor.com/santaba/rpc/getHost?c=$company&u=$api_user&p=$api_pass&displayName=$displayname")

def hostInfoJSON = new JsonSlurper().parseText(hostInfo)
def id = hostInfoJSON.data.id
String ips = hostInfoJSON.data.properties.get "system.ips"

def host = ips.split(",")[0]
def pingCommand = """ping  -n $qty_of_pings_per_batch $host"""
pingCommand = """ping  -n $qty_of_pings_per_batch 192.168.1.1"""


//    println "hostname: $host"
int summLoss = 0
int summAve = 0

//cycles for ping
for (int i = 0; i < qty_of_batches; i++) {
    def proc = pingCommand.execute()
    proc.waitFor()
//    println "return code: ${proc.exitValue()}"

    def buffer_contents = proc.in.text

    String lostString
    String aproximate
    buffer_contents.trim().split("\n").each {
        if (it.length() > 1) {
            if (it.contains("loss"))
                lostString = it.trim()
            if (it.contains("Average"))
                aproximate = it.trim()
        }
    }

//println lostString
//println aproximate

    String lostMS = new StringBuffer(lostString.toString()).substring(lostString.indexOf('(') + 1, lostString.indexOf(')')).split("loss")[0].split("%")[0]
//    println "lostMS:$lostMS"

    summLoss = summLoss + Integer.valueOf(lostMS)

    String averagef
    if (aproximate != null) {
        averagef = aproximate.substring(aproximate.lastIndexOf('=') + 1, aproximate.length()).split("ms")[0].trim()
//        println "average:$averagef"
        summAve = summAve + Integer.valueOf(averagef)
    }


    sleep(seconds_between_batches * 1000l)
}

summAve = summAve / qty_of_batches
summLoss = summLoss / qty_of_batches

//println "sumAve:$summAve"
//println "sumLose:$summLoss"

if(summLoss < threshold_ping_loss_percent && summAve < threshold_average_ms){
    println "workFine: $summAve $summLoss"
    return 0 ;
}
