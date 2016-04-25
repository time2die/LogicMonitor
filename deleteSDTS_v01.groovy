import com.santaba.agent.groovyapi.expect.Expect;
import com.santaba.agent.groovyapi.snmp.Snmp;
import com.santaba.agent.groovyapi.http.*;
import com.santaba.agent.groovyapi.jmx.*;
import org.xbill.DNS.*;
import groovy.json.JsonSlurper
def company = "suding"
def api_user = "api4"
def api_pass = "api.805"
def ids = [86,77]
println "delete SDT for $ids"
ids.each {
    String deleteReq = HTTP.body("https://" + company + ".logicmonitor.com/santaba/rpc/deleteSDTs?c=$company&amp;u=$api_user&amp;p=$api_pass&amp;type=group&amp;hostGroupId=$it")
    def delete = new JsonSlurper().parseText(deleteReq).data
    println "delete:$deleteReq"
}
ids.each {
    String listReq = HTTP.body("https://$company"+".logicmonitor.com/santaba/rpc/getSDTs?c=$company&u=$api_user&p=$api_pass&hostGroupId=$it")
    def info = new JsonSlurper().parseText(listReq).data
    println "STD::$info"
}
