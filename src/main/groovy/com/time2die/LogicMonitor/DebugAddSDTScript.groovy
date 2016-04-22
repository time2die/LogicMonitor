import groovy.json.JsonSlurper

import java.text.SimpleDateFormat

class DebugAddSDTScript {


    DebugAddSDTScript() {
        HTTP.put("body", body)
    }

    def HTTP = new HashMap();

    def body = { String url ->
        return new URL(url).getText();
    }

    public static void main(String[] args) {
        new DebugAddSDTScript().work()
    }

    public static String generateURL() {
    }

    public void work() {
        def my_displayname = "EdgeRouter" //hard coded for testing in LogicMonitor debug window
        def company = "suding"
        def api_user = "api4"
        def api_pass = "api.805"
        def displayname = URLEncoder.encode(my_displayname, "UTF-8")
        def SDTTimeInMinute = 60

        String hostInfo = HTTP.body("https://" + company + ".logicmonitor.com/santaba/rpc/getHost?c=$company&u=$api_user&p=$api_pass&displayName=$displayname")
        def hostInfoJSON = new JsonSlurper().parseText(hostInfo)
        def hostId = hostInfoJSON.data.id
        def groupid = hostInfoJSON.data.fullPathInIds

        def last_item = groupid.last()  // this somehow removes the outer brackets
        def my_groupid = last_item.last()
        println "This device is in GroupID $my_groupid"


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

        println "Found ${subgroupListName.size} subgroups under groupID $my_groupid"
        subgroupListName.each {
            println it
        }

        [86,77].each {
            String deleteReq = HTTP.body("https://" + company + ".logicmonitor.com/santaba/rpc/deleteSDTs?c=$company&u=$api_user&p=$api_pass&type=group&hostGroupId=$it")
            def delete = new JsonSlurper().parseText(deleteReq).data
            println "delete:$deleteReq"
        }

        subgroupsIds.each {
//            println it


            String sdts = HTTP.body("https://" + company + ".logicmonitor.com/santaba/rpc/getSDTs?c=$company&u=$api_user&p=$api_pass&=hostGroupId=$it")
            def sdtsResposne = new JsonSlurper().parseText(sdts).data
//            println sdts
//            println sdtsResposne.hostGroupId
            def sdtsList = sdtsResposne.hostGroupId
            if (sdtsList.empty || !sdtsList.contains(it)) {
//                println "work"
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
//                println setSdts
                SimpleDateFormat df = new SimpleDateFormat();
                //2016:04:16:11:23
                df.applyPattern("yyyy:MM:dd:HH:mm");
                def prettyDate = df.format(sdtEndTime.getTime())
                println "Setting SDT on group id $it for $SDTTimeInMinute minutes (now to $prettyDate)"
            }

            }
        }
    }

