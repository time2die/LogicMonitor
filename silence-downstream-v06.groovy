import com.santaba.agent.groovyapi.http.*
import com.santaba.agent.groovyapi.jmx.*
import groovy.json.JsonSlurper
import org.xbill.DNS.*

import java.text.SimpleDateFormat

boolean needDebug = false;

def my_displayname = "EdgeRouter"
def displayname = URLEncoder.encode(my_displayname, "UTF-8")
def company = "suding"
def api_user = "api4"
def api_pass = "api.805"

def qty_of_pings_per_batch = 3
def qty_of_batches = 2
def seconds_between_batches = 1

def threshold_average_ms = 77
def threshold_ping_loss_percent = 70

String readReq(String url, boolean needDebug) {
    String req = HTTP.body(url)

    if (needDebug) {
        print "request: $url\n"
        print "respone: $req\n"
    }

    return req;
}

Object parseJSONObject(String data) {
    return new JsonSlurper().parseText(data)
}

def hostInfoJSON = parseJSONObject(readReq("https://" + company + ".logicmonitor.com/santaba/rpc/getHost?c=$company&u=$api_user&p=$api_pass&displayName=$displayname", needDebug))

String ips = hostInfoJSON.data.properties.get "system.ips"

def host = ips.split(",")[0]
print "hostname=$host\n"

def pingCommand = """ping  -n $qty_of_pings_per_batch $host"""
// you can use 192.168.1.1 to test negative scnario
if (needDebug) {
    pingCommand = """ping  -n $qty_of_pings_per_batch 192.168.1.1"""
}

int summLoss = 0
int summAve = 0
int qty_apr = 1;

int pingIter = 1
for (int i = 1; i < qty_of_batches + 1; i++) {

    if (i > 1) {
        print "Waited $seconds_between_batches seconds\n"
        sleep(seconds_between_batches * 1000l)
    }

    def proc = pingCommand.execute()
    proc.waitFor()

    def buffer_contents = proc.in.text

    String lostString
    String aproximate

    // print "buffer_contents:$buffer_contents"
    buffer_contents.trim().split("\n").each {
        // println "each:$it"
        if (it.length() > 1) {
            if (it.contains("loss"))
                lostString = it.trim()
            if (it.contains("time")) {
                aproximate = it.trim().split("time")[1].substring(1).split("ms")[0].trim()
                if (aproximate.indexOf(' ') == -1 || aproximate.indexOf('.') != -1) {
                    int a = 0;
                    try {
                        a = Integer.valueOf(aproximate)
                        qty_apr++;
                    } catch (NumberFormatException e) {
//                        println "ap:$aproximate"
//                        println e ;
                    }
                    summAve = summAve + a
                }
            }
        }
    }

    def pings = buffer_contents.split("\n")
    pings = pings.findAll { it.length() > 2 }
    for (int pingIterator = 1; pingIterator < qty_of_pings_per_batch + 1; pingIterator++) {
        if (pings[pingIterator].indexOf("out.") != -1) {
            pingIter = pingIter + 1;
            print "${pingIter} request timed out\n"
        } else {
            // print  pings[pingIterator]
            def ms = pings[pingIterator].split("time")[1].split(" ")[0]
            ms = ms.substring(1)
            print "${pingIter} ping time $ms\n"
            pingIter = pingIter + 1;
        }
    }

    String lostMS = new StringBuffer(lostString.toString()).substring(lostString.indexOf('(') + 1, lostString.indexOf(')')).split("loss")[0].split("%")[0]
    summLoss = summLoss + Integer.valueOf(lostMS)

    String averagef
//
//    if (aproximate != null) {
//        averagef = aproximate.substring(aproximate.lastIndexOf('=') + 1, aproximate.length()).split("ms")[0].trim()
//        println "averagef:$averagef"
//        summAve = summAve + Integer.valueOf(averagef)
//    }

}
try {
    summAve = summAve / qty_apr
} catch (Exception e) {
}
try {
    summLoss = summLoss / qty_of_batches
} catch (Exception e) {
}

if (summLoss != 100) {
    summAve = summAve > 0 ? summAve : 1;
}
print "Average ping time (ms):$summAve\n"
print "Threshold_average_ms: $threshold_average_ms\n"
print "Loss_percent:$summLoss\n"


if (summLoss < threshold_ping_loss_percent && summAve < threshold_average_ms) {
    print "Average ping time and loss percent are under threshold, so exiting\n"
    return 1;
}

if (summAve > threshold_average_ms) {
    print "Average ping time is above threshold, so get subgroups then set SDT\n"
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
boolean return3 = false;
subgroupsIds.each {

    String sdts =  readReq("https://" + company + ".logicmonitor.com/santaba/rpc/getSDTs?c=$company&u=$api_user&p=$api_pass&=hostGroupId=$it", needDebug)

    def sdtsResposne = parseJSONObject(sdts).data
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
    if (sdtsList.empty || sdtsList.contains(it)) {

        def isEffective = sdtsResposne.isEffective
        def isEffectiveIndex = [].indexOf(it)
        def effective = isEffective[isEffectiveIndex]
        if (effective) {
            print "SDT is already set on groupID $it, so don’t set SDT"
            return3 = true
        }
    }

}
//script finishing
print "Script finished\n"
if (return3) {
    return 3
} else {
    return 2
}