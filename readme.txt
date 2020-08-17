ReadMe
--------------------------------------------------------------------------------

(1) Master
	a. Install MySQL database
		Create 'kit418' schema (empty)
		Set root password as 'toor'
	b. Upload .ppk files, run.sh, web-0.1.0.jar and Master.jar to /home/ubuntu
	c. Start Web application
		Execute run.sh
	d. Start Master application
		Execute command: java -jar Master.jar <SCHEDULING>
			SCHEDULING: 1	Round robin
						2	Free worker first

(2) Worker
	a. Upload killProcess.sh, Instance.jar to /home/ubuntu
	b. Create folder /home/ubuntu/Pending, /home/ubuntu/Sources, /home/ubuntu/Outputs
	c. Start Worker application
		Execute command: java -jar Instance [InstanceID] <MasterIP> <MasterPort>