import com.santaba.agent.groovyapi.expect.Expect;
import com.santaba.agent.groovyapi.snmp.Snmp;
import com.santaba.agent.groovyapi.http.*;
import com.santaba.agent.groovyapi.jmx.*;
import org.xbill.DNS.*;
import groovy.json.JsonSlurper

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


for (int i = 0; i < qty_of_batches; i++) {
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
    pings = pings.findAll {it.length() > 2}
    for(int pingIterator = 1 ; pingIterator < qty_of_pings_per_batch +1 ; pingIterator ++){
        def ms = pings[pingIterator].split("time")[1].split(" ")[0]
        ms = ms.substring(1)
        print "${i*pingIterator} ping time $ms"
    }

    String lostMS = new StringBuffer(lostString.toString()).substring(lostString.indexOf('(') + 1, lostString.indexOf(')')).split("loss")[0].split("%")[0]
    summLoss = summLoss + Integer.valueOf(lostMS)

    String averagef
    if (aproximate != null) {
        averagef = aproximate.substring(aproximate.lastIndexOf('=') + 1, aproximate.length()).split("ms")[0].trim()

        summAve = summAve + Integer.valueOf(averagef)
    }


    sleep(seconds_between_batches * 1000l)
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

def hostId = hostInfoJSON.data.id
def groupid = hostInfoJSON.data.fullPathInIds

def last_item = groupid.last()  // this somehow removes the outer brackets
println "last_item is  $last_item"
def my_groupid = last_item.last()
println "my_groupid is $my_groupid"

String sdts = HTTP.body("https://" + company + ".logicmonitor.com/santaba/rpc/getSDTs?c=$company&u=$api_user&p=$api_pass&=hostId=$hostId")
def sdtsResposne = new JsonSlurper().parseText(sdts).data
println sdts
def sdtsList = sdtsResposne.data
println sdtsResposne
println sdtsList.empty

if (sdtsList.empty) {
//        need add new sdt
    def now = Calendar.getInstance()
    def sdtEndTime = Calendar.getInstance()
    sdtEndTime.add(Calendar.MINUTE, SDTTimeInMinute)
    String setSdtBody = new String("https://" + company + ".logicmonitor.com/santaba/rpc/setHostGroupSDT?c=$company&u=$api_user&p=$api_pass&=hostId=$hostId&type=1"
            + "&year=${now.get(Calendar.YEAR)}&month=${now.get(Calendar.MONTH)}&day=${now.get(Calendar.DAY_OF_MONTH)}&hour=${now.get(Calendar.HOUR)}&minute=${now.get(Calendar.MINUTE)}" +
            "&endYear=${sdtEndTime.get(Calendar.YEAR)}&endMonth=${sdtEndTime.get(Calendar.MONTH)}&endDay=${sdtEndTime.get(Calendar.DAY_OF_MONTH)}&endHour=${sdtEndTime.get(Calendar.HOUR)}&endMinute=${sdtEndTime.get(Calendar.MINUTE)}" +
            "&hostGroupId=$my_groupid")
    String setSdts = HTTP.body(setSdtBody)
}

