package com.time2die.LogicMonitor

def s = "helloWorld"

void test(){
    println(this:s)
}

void printlnHell(String sss){
    println sss
}

def fileName = new Date().toGMTString().trim().replace(' ','_').replace(':','-')+".log"
printlnHell(fileName)
test()
//File f = new File(fileName)
//f << "example\n"
//f << "second\n"
//f << "ttt\n"
