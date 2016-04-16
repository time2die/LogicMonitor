import com.santaba.agent.groovyapi.http.*
import com.santaba.agent.groovyapi.jmx.*
import groovy.json.JsonSlurper
import org.xbill.DNS.*


def minutes_of_SDT = 60

def my_displayname = "EdgeRouter"

def company = "suding"
def api_user = "api4"
def api_pass = "api.805"

def qty_of_pings_per_batch = 3
def qty_of_batches = 2
def seconds_between_batches = 9
def threshold_average_ms = 77
def threshold_ping_loss_percent = 70


def displayname = URLEncoder.encode(my_displayname, "UTF-8")

String hostInfo = HTTP.body("https://" + company + ".logicmonitor.com/santaba/rpc/getHost?c=$company&u=$api_user&p=$api_pass&displayName=$displayname")


def hostInfoJSON = new JsonSlurper().parseText(hostInfo)
def id = hostInfoJSON.data.id
String ips = hostInfoJSON.data.properties.get "system.ips"

def host = ips.split(",")[0]


println "hostname: $host"
def mycommand = """ping  -n $qty_of_pings_per_batch $host"""
def proc = mycommand.execute()
proc.waitFor()
println "return code: ${proc.exitValue()}"

def buffer_contents = proc.in.text
println buffer_contents
def average = buffer_contents.split("Average = ")

if (average.size() > 1) {
    def msAverage = average[1]
    println "ms: $msAverage"
}



