import com.santaba.agent.groovyapi.http.*
import com.santaba.agent.groovyapi.jmx.*
import groovy.json.JsonSlurper
import org.xbill.DNS.*
import java.text.SimpleDateFormat

def my_displayname = "EdgeRouter"
def company = "suding"
def api_user = "api4"
def api_pass = "api.805"

def qty_of_pings_per_batch = 3;
def qty_of_batches = 2;
def seconds_between_batches = 1;

def threshold_average_ms = 77;
def threshold_ping_loss_percent = 70;

def displayname = URLEncoder.encode(my_displayname, "UTF-8")
String hostInfo = HTTP.body("https://" + company + ".logicmonitor.com/santaba/rpc/getHost?c=$company&u=$api_user&p=$api_pass&displayName=$displayname")

def hostInfoJSON = new JsonSlurper().parseText(hostInfo)
def id = hostInfoJSON.data.id
String ips = hostInfoJSON.data.properties.get "system.ips"

def host = ips.split(",")[0]
print "hostname=$host\n"

def pingCommand = """ping  -n $qty_of_pings_per_batch $host"""
// you can use 192.168.1.1 to test negative scnario
// pingCommand = """ping  -n $qty_of_pings_per_batch 192.168.1.1"""

int summLoss = 0
int summAve = 0

int pingIter = 1
for (int i = 1; i < qty_of_batches + 1; i++) {

    if (i > 1) {
        print "Waited $seconds_between_batches seconds\n"
        sleep(seconds_between_batches * 1000l)
    }

    def proc = pingCommand.execute()
    proc.waitFor()

    def buffer_contents = proc.in.text
//    println buffer_contents

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

    def pings = buffer_contents.split("\n")
    pings = pings.findAll { it.length() > 2 }
    for (int pingIterator = 1; pingIterator < qty_of_pings_per_batch + 1; pingIterator++) {
        def ms = pings[pingIterator].split("time")[1].split(" ")[0]
        ms = ms.substring(1)
        print "${pingIter} ping time $ms\n"
        pingIter = pingIter +1 ;
    }

    String lostMS = new StringBuffer(lostString.toString()).substring(lostString.indexOf('(') + 1, lostString.indexOf(')')).split("loss")[0].split("%")[0]
    summLoss = summLoss + Integer.valueOf(lostMS)

    String averagef
    if (aproximate != null) {
        averagef = aproximate.substring(aproximate.lastIndexOf('=') + 1, aproximate.length()).split("ms")[0].trim()

        summAve = summAve + Integer.valueOf(averagef)
    }

}

summAve = summAve / qty_of_batches
summLoss = summLoss / qty_of_batches

print "Average ping time (ms):$summAve\n"
print "Threshold_average_ms: $threshold_average_ms\n"
print "Loss_percent:$summLoss\n"

if (summLoss < threshold_ping_loss_percent && summAve < threshold_average_ms) {
    print "Average ping time and loss percent are under threshold, so exiting\n"
    return;
}
def SDTTimeInMinute = 60


def groupid = hostInfoJSON.data.fullPathInIds

def last_item = groupid.last()  // this somehow removes the outer brackets
def my_groupid = last_item.last()
print "This device is in GroupID $my_groupid\n"


String devGroupBody = HTTP.body("https://" + company + ".logicmonitor" +
        ".com/santaba/rpc/getHostGroup?c=$company&u=$api_user&p=$api_pass&hostGroupId=$my_groupid")
//        println "dev:$devGroupBody"


String subgroupsReq = HTTP.body("https://" + company +
        ".logicmonitor.com/santaba/rpc/getHostGroupChildren?c=$company&u=$api_user&p=$api_pass&hostGroupId=$my_groupid")
def subgroups = new JsonSlurper().parseText(subgroupsReq).data.items

//        println subgroups
def subgroupListName = []
def subgroupsIds = []
subgroups.each {
    String subGroupInfoReq = HTTP.body("https://" + company + ".logicmonitor.com/santaba/rpc/getHostGroup?c=$company&u=$api_user&p=$api_pass&hostGroupId=${it.id}")
    def subGroupInfo = new JsonSlurper().parseText(subGroupInfoReq).data

    if (subGroupInfo != null) {
        subgroupsIds << it.id
        subgroupListName << "Subgroup named \"${subGroupInfo.name}\" is groupID ${it.id}"
    }
}

print "Found ${subgroupListName.size} subgroups under groupID $my_groupid\n"
subgroupListName.each {
    print "$it\n"
}

subgroupsIds.each {

    String sdts = HTTP.body("https://" + company + ".logicmonitor.com/santaba/rpc/getSDTs?c=$company&u=$api_user&p=$api_pass&=hostGroupId=$it")
    def sdtsResposne = new JsonSlurper().parseText(sdts).data
    def sdtsList = sdtsResposne.hostGroupId
    if (sdtsList.empty || !sdtsList.contains(it)) {
        def now = Calendar.getInstance()
        def sdtEndTime = Calendar.getInstance()
        sdtEndTime.add(Calendar.MINUTE, SDTTimeInMinute)
        String setSdtBody = new String("https://" + company + ".logicmonitor.com/santaba/rpc/setHostGroupSDT?c=$company&u=$api_user&p=$api_pass&hostGroupId=$it&type=1" +
                "&year=${now.get(Calendar.YEAR)}" +
                "&month=${now.get(Calendar.MONTH)}" +
                "&day=${now.get(Calendar.DAY_OF_MONTH)}" +
                "&hour=${now.get(Calendar.HOUR_OF_DAY)}" +
                "&minute=${now.get(Calendar.MINUTE)}" +
                "&endYear=${sdtEndTime.get(Calendar.YEAR)}" +
                "&endMonth=${sdtEndTime.get(Calendar.MONTH)}" +
                "&endDay=${sdtEndTime.get(Calendar.DAY_OF_MONTH)}" +
                "&endHour=${sdtEndTime.get(Calendar.HOUR_OF_DAY)}" +
                "&endMinute=${sdtEndTime.get(Calendar.MINUTE)}")
        String setSdts = HTTP.body(setSdtBody)

        SimpleDateFormat df = new SimpleDateFormat();
        //2016:04:16:11:23
        df.applyPattern("yyyy:MM:dd:HH:mm");
        def prettyDate = df.format(sdtEndTime.getTime())
        print "Setting SDT on group id $it for $SDTTimeInMinute minutes (now to $prettyDate)\n"
    }
}
