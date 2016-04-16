package com.time2die.LogicMonitor
//import com.santaba.agent.groovyapi.http.*;

import groovy.json.JsonSlurper
class Main {

    Main(){
        HTTP.put("body",body)
    }

    def HTTP = new HashMap() ;

    def body = { String url ->
        return new URL(url).getText() ;
    }

    public static void main(String[] args){
          new Main().work()
    }


    public static void ping (String host){
        println "hostname: $host"
        //temporarily remmed out this section during testing to make it faster
        def mycommand = """ping  -c 1 $host"""    // Create the command
        def proc = mycommand.execute()                 // *execute* the command
        proc.waitFor()                               // Wait for the command to finish
        println "return code: ${ proc.exitValue()}" // get status and output
        println "stderr: ${proc.err.text}"  // print errors if any

        def buffer_contents = proc.in.text
        println buffer_contents
        def Average = (buffer_contents =~ "Average =(.*)")
 //       def value = Average[0][1].toString().replace("ms", "").toInteger();
        println "The average i found is $Average"

    }

    def work(){
        def my_displayname="EdgeRouter" //hard coded for testing in LogicMonitor debug window
        def company="suding"
        def api_user="api4"
        def api_pass="api.805"
        def displayname = URLEncoder.encode(my_displayname, "UTF-8")

        String hostInfo = HTTP.body("https://" + company + ".logicmonitor.com/santaba/rpc/getHost?c=$company&u=$api_user&p=$api_pass&displayName=$displayname")
        //println hostInfo
        def hostInfoJSON = new JsonSlurper().parseText(hostInfo)

        def id = hostInfoJSON.data.id
        String ips = hostInfoJSON.data.properties.get "system.ips"
        ips.split(",").each {
            ping (it)
            println "ip:$it"
        }

        //println "the host id is: $id"  // this gives 520
        def groupid = hostInfoJSON.data.fullPathInIds
        //println "the list of group ids is: $groupid"  //  This gives [[75, 76]] which seems to be an ArrayList.
        def last_item = groupid.last()  // this somehow removes the outer brackets
        //println "last_item is  $last_item"
        def my_groupid = last_item.last()
        //println "my_groupid is $my_groupid"


        String subgroups = HTTP.body("https://" + company + ".logicmonitor.com/santaba/rpc/getHostGroupChildren?c=$company&u=$api_user&p=$api_pass&hostGroupId=$my_groupid")

        /*//println subgroups

        def subgroupsJSON = new JsonSlurper().parseText(subgroups)
        def subgroupsList = subgroupsJSON.data

        subgroupsList.each{
            println ""
            println it
            println ""
        }*/
        //println "this is subgroupslist $subgroupsList"

// This FOR loop doesn't work yet.
/*
        for(name in subgroupsList) {
            subgroupsList.each {
               def  my_name = it.name
                println "my name is  $my_name"
            }
        }
*/
    }

}
