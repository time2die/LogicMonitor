import groovy.json.JsonSlurper

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
//        println "the list of group ids is: $groupid"  //  This gives [[75, 76]] which seems to be an ArrayList.
        def last_item = groupid.last()  // this somehow removes the outer brackets
        println "last_item is  $last_item"
        def my_groupid = last_item.last()
        println "my_groupid is $my_groupid"


        String devGroupBody = HTTP.body("https://" + company + ".logicmonitor.com/santaba/rpc/getHostGroup?c=$company&u=$api_user&p=$api_pass&hostGroupId=76")
//        println "dev:$devGroupBody"

        devGroupBody = HTTP.body("https://" + company + ".logicmonitor.com/santaba/rpc/getHostGroup?c=$company&u=$api_user&p=$api_pass&hostGroupId=75")
//        println "dev:$devGroupBody"

//        String subgroups = HTTP.body("https://" + company + ".logicmonitor.com/santaba/rpc/getHostGroupChildren?c=$company&u=$api_user&p=$api_pass&hostGroupId=$my_groupid")

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
//        println "addSDT:$setSdtBody"
//        println "add one"
        } else {
            println sdts
            println "already have one:$sdtsList"
        }
    }
}

