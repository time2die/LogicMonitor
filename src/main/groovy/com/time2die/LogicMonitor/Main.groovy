package com.time2die.LogicMonitor
//import com.santaba.agent.groovyapi.http.*;

import groovy.json.JsonSlurper

class Main {

    Main() {
        HTTP.put("body", body)
    }

    def HTTP = new HashMap();

    def body = { String url ->
        return new URL(url).getText();
    }

    public static void main(String[] args) {
        new Main().work()
    }


    def work() {
        def my_displayname = "EdgeRouter" //hard coded for testing in LogicMonitor debug window
        def company = "suding"
        def api_user = "api4"
        def api_pass = "api.805"
        def displayname = URLEncoder.encode(my_displayname, "UTF-8")

        String hostInfo = HTTP.body("https://" + company + ".logicmonitor.com/santaba/rpc/getHost?c=$company&u=$api_user&p=$api_pass&displayName=$displayname")
        //println hostInfo
        def hostInfoJSON = new JsonSlurper().parseText(hostInfo)
//        println "the host id is: $id"  // this gives 520
        def groupid = hostInfoJSON.data.fullPathInIds
        println "the list of group ids is: $groupid"  //  This gives [[75, 76]] which seems to be an ArrayList.
        def last_item = groupid.last()  // this somehow removes the outer brackets
        //println "last_item is  $last_item"
        def my_groupid = last_item.last()
        //println "my_groupid is $my_groupid"

        String subgroups = HTTP.body("https://" + company + ".logicmonitor.com/santaba/rpc/getHostGroupChildren?c=$company&u=$api_user&p=$api_pass&hostGroupId=$my_groupid")

//        println subgroups

        def subgroupsJSON = new JsonSlurper().parseText(subgroups)
        def subgroupsList = subgroupsJSON.data.items

        subgroupsList.each {
            println "id:${it.id}"
            println ">$it<"
            println ""
        }
        //println "this is subgroupslist $subgroupsList"
    }

}
