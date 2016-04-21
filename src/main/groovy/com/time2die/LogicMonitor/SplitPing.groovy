def text = """
Pinging 10.9.8.1 with 32 bytes of data:

Reply from 10.9.8.1: bytes=32 time<1ms TTL=64

Reply from 10.9.8.1: bytes=32 time=4ms TTL=64

Reply from 10.9.8.1: bytes=32 time=1ms TTL=64



Ping statistics for 10.9.8.1:

    Packets: Sent = 3, Received = 3, Lost = 0 (0% loss),

Approximate round trip times in milli-seconds:

    Minimum = 0ms, Maximum = 4ms, Average = 1ms
"""

text = """Pinging 192.168.1.1 with 32 bytes of data:

Request timed out.

Request timed out.

Request timed out.



Ping statistics for 192.168.1.1:

    Packets: Sent = 3, Received = 0, Lost = 3 (100% loss),


"""

String lostString
String aproximate
text.trim().split("\n").each {
    if(it.length() > 1) {
        if (it.contains("loss"))
            lostString = it.trim()
        if(it.contains("Average"))
            aproximate = it.trim()
    }
}

println lostString
println aproximate

String lostMS = new StringBuffer(lostString.toString()).substring(lostString.indexOf('(')+1,lostString.indexOf(')')).split("loss")[0].split("%")[0]
println lostMS

if(aproximate != null) {
    String average = aproximate.substring(aproximate.lastIndexOf('=') + 1, aproximate.length()).split("ms")[0].trim()
    println average
}

def future = Calendar.getInstance().add(Calendar.MINUTE,60).toString()
def nowCal = Calendar.instance.toString()
println "$nowCal:$future"