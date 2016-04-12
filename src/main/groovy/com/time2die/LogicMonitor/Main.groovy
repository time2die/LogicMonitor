package com.time2die.LogicMonitor

import groovy.json.JsonSlurper
//import com.santaba.agent.groovyapi.http.*;
/**
 * Created by time2die on 13.04.16.
 */
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

    def work(){
        def my_displayname="EdgeRouter" //hard coded for testing in LogicMonitor debug window
        def company="suding"
        def api_user="api4"
        def api_pass="api.805"
        def displayname = URLEncoder.encode(my_displayname, "UTF-8")

// temporarily remmed out this section during testing to make it faster
/// def mycommand = """ping -n 2 wowie.us"""    // Create the command
/// def proc = mycommand.execute()                 // *execute* the command
/// proc.waitFor()                               // Wait for the command to finish
//println "return code: ${ proc.exitValue()}" // get status and output
//println "stderr: ${proc.err.text}"  // print errors if any
/// def buffer_contents = proc.in.text
/// println buffer_contents
/// def Average = (buffer_contents =~ "Average =(.*)")
/// def value = Average[0][1].toString().replace("ms", "").toInteger();
/// println "The average i found is $value"

        String hostInfo = HTTP.body("https://" + company + ".logicmonitor.com/santaba/rpc/getHost?c=$company&u=$api_user&p=$api_pass&displayName=$displayname")
        def hostInfoJSON = new JsonSlurper().parseText(hostInfo)
        def id = hostInfoJSON.data.id
        println "the host id is: $id"  // this gives 520
        def groupid = hostInfoJSON.data.fullPathInIds
        println "the list of group ids is: $groupid"  //  This gives [[75, 76]] which seems to be an ArrayList.
        def last_item = groupid.last()  // this somehow removes the outer brackets
        println "last_item is  $last_item"
        def my_groupid = last_item.last()
        println "my_groupid is $my_groupid"


        String subgroups = HTTP.body("https://" + company + ".logicmonitor.com/santaba/rpc/getHostGroupChildren?c=$company&u=$api_user&p=$api_pass&hostGroupId=$my_groupid")
        //println subgroups

        def subgroupsJSON = new JsonSlurper().parseText(subgroups)
        def subgroupsList = subgroupsJSON.data

        subgroupsList.each{
            println ""
            println it
            println ""
        }

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
