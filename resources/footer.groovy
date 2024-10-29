
job = new MVSJob()
job.start()

for(s in steps){
	s()
}

job.stop()

}catch(Exception e){
	println("Exception in step: $stepname")
	e.printStackTrace()
}
