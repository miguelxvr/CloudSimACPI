###################
# defines
#################

library(ggplot2)
library(doBy)
library(scales)
  
summarySE <- function(data=NULL, measurevar, groupvars=NULL, na.rm=FALSE, conf.interval=.95) {
      require(doBy)
  
      # New version of length which can handle NA's: if na.rm==T, don't count them
      length2 <- function (x, na.rm=FALSE) {
          if (na.rm) sum(!is.na(x))
          else       length(x)
      }
  
      # Collapse the data
      formula <- as.formula(paste(measurevar, paste(groupvars, collapse=" + "), sep=" ~ "))
      datac <- summaryBy(formula, data=data, FUN=c(length2,mean,sd), na.rm=na.rm)
  
      # Rename columns
      names(datac)[ names(datac) == paste(measurevar, ".mean",    sep="") ] <- measurevar
      names(datac)[ names(datac) == paste(measurevar, ".sd",      sep="") ] <- "sd"
      names(datac)[ names(datac) == paste(measurevar, ".length2", sep="") ] <- "N"
      
      datac$se <- datac$sd / sqrt(datac$N)  # Calculate standard error of the mean
            
      # Confidence interval multiplier for standard error
      # Calculate t-statistic for confidence interval: 
      # e.g., if conf.interval is .95, use .975 (above/below), and use df=N-1
      ciMult <- qt(conf.interval/2 + .5, datac$N-1)
      datac$ci <- datac$se * ciMult
      
      return(datac)
}


improvement <- function(baseline=NULL, value=NULL)
{
	# Example:
	# If I have something that took 94 minutes to complete and I got it down to 62 minutes
	# The baseline for comparison is 94 minutes. 
	# The improvement is (94-62)=32 minutes.
	# 32/94 = .340426 ≅ 34%

	improv <- (baseline-value)
	percent <- improv/baseline
	
	return(percent)
}

########################################################################
## Dados reais - poweroff
########################################################################

postscript("results-poweroff-linhas-real.eps", horizontal = FALSE, onefile = FALSE, paper = "special", width = 7, height = 5)

dados<-read.table("result.sleepstate.real.poweroff.txt", header=TRUE, sep=";")
#dfc <- summarySE(dados, measurevar="watts", groupvars=c("host", "time"))

dados <- rbind(dados,data.frame(watts = 20, time = 0, host = "sim-host1"))
dados <- rbind(dados,data.frame(watts = 160, time = 0, host = "sim-host1"))
dados <- rbind(dados,data.frame(watts = 160, time = 122, host = "sim-host1"))
dados <- rbind(dados,data.frame(watts = 210, time = 122, host = "sim-host1"))
dados <- rbind(dados,data.frame(watts = 210, time = 2122, host = "sim-host1"))
dados <- rbind(dados,data.frame(watts = 60, time = 2122, host = "sim-host1"))
dados <- rbind(dados,data.frame(watts = 60, time = 2128, host = "sim-host1"))
dados <- rbind(dados,data.frame(watts = 20, time = 2128, host = "sim-host1"))
dados <- rbind(dados,data.frame(watts = 20, time = 3028, host = "sim-host1"))

ggplot(dados, aes(x=time, y=watts, colour=host, shape=host, group=host, linetype=host)) + 
        #geom_point(size = 2.5) +
    geom_line(size=1) +
    scale_linetype_manual(values=c(1,1,1,1,2,3,4,6))+
	scale_y_continuous(limits=c(20, 300), breaks=seq(20, 300, by=50)) +
	scale_color_manual(values=c("real-host1"="#f768a1", "real-host2"="#78c679", "real-host3"="#74a9cf", "real-host4"="#fb6a4a","sim-host1"="black", "sim-host2"="black", "sim-host3"="black", "sim-host4"="black")) +
	theme_bw() +
	theme(legend.title=element_blank(), text = element_text(size=15)) +
	annotate("text", x=150, y=250, label="A", color = "black") +
	geom_vline(xintercept=300, linetype="dotted") + 
	annotate("text", x=355, y=250, label="B", color = "black") +
	geom_vline(xintercept=420, linetype="dotted") + 
	annotate("text", x=940, y=250, label="C", color = "black") +
	geom_vline(xintercept=2422, linetype="dotted") + 
	annotate("text", x=1800, y=250, label="D", color = "black") +

	#geom_vline(xintercept=1620, linetype="dotted") + 
	#geom_vline(xintercept=1920, linetype="dotted") + 
	#geom_vline(xintercept=2220, linetype="dotted") + 
	xlab("Execution Time (seconds)") +
	ylab("Energy Consumption (Wh)")
     
dev.off()

########################################################################
## Dados reais - hibernate
########################################################################

postscript("results-hibernate-linhas-real.eps", horizontal = FALSE, onefile = FALSE, paper = "special", width = 7, height = 5)

dados<-read.table("result.sleepstate.real.hibernate.txt", header=TRUE, sep=";")
#dfc <- summarySE(dados, measurevar="watts", groupvars=c("host", "time"))

dados <- rbind(dados,data.frame(watts = 20, time = 0, host = "sim-host1"))
dados <- rbind(dados,data.frame(watts = 190, time = 0, host = "sim-host1"))
dados <- rbind(dados,data.frame(watts = 190, time = 142, host = "sim-host1"))
dados <- rbind(dados,data.frame(watts = 210, time = 142, host = "sim-host1"))
dados <- rbind(dados,data.frame(watts = 210, time = 2142, host = "sim-host1"))
dados <- rbind(dados,data.frame(watts = 100, time = 2153, host = "sim-host1"))
dados <- rbind(dados,data.frame(watts = 20, time = 2153, host = "sim-host1"))
dados <- rbind(dados,data.frame(watts = 20, time = 3053, host = "sim-host1"))

dados <- rbind(dados,data.frame(watts = 20, time = 0, host = "sim-host2"))
dados <- rbind(dados,data.frame(watts = 20, time = 300, host = "sim-host2"))
dados <- rbind(dados,data.frame(watts = 190, time = 300, host = "sim-host2"))
dados <- rbind(dados,data.frame(watts = 190, time = 442, host = "sim-host2"))
dados <- rbind(dados,data.frame(watts = 210, time = 442, host = "sim-host2"))
dados <- rbind(dados,data.frame(watts = 210, time = 2442, host = "sim-host2"))
dados <- rbind(dados,data.frame(watts = 100, time = 2453, host = "sim-host2"))
dados <- rbind(dados,data.frame(watts = 20, time = 2453, host = "sim-host2"))
dados <- rbind(dados,data.frame(watts = 20, time = 3053, host = "sim-host2"))

dados <- rbind(dados,data.frame(watts = 20, time = 0, host = "sim-host3"))
dados <- rbind(dados,data.frame(watts = 20, time = 600, host = "sim-host3"))
dados <- rbind(dados,data.frame(watts = 190, time = 600, host = "sim-host3"))
dados <- rbind(dados,data.frame(watts = 190, time = 742, host = "sim-host3"))
dados <- rbind(dados,data.frame(watts = 210, time = 742, host = "sim-host3"))
dados <- rbind(dados,data.frame(watts = 210, time = 2742, host = "sim-host3"))
dados <- rbind(dados,data.frame(watts = 100, time = 2753, host = "sim-host3"))
dados <- rbind(dados,data.frame(watts = 20, time = 2753, host = "sim-host3"))
dados <- rbind(dados,data.frame(watts = 20, time = 3053, host = "sim-host3"))

dados <- rbind(dados,data.frame(watts = 20, time = 0, host = "sim-host4"))
dados <- rbind(dados,data.frame(watts = 20, time = 900, host = "sim-host4"))
dados <- rbind(dados,data.frame(watts = 190, time = 900, host = "sim-host4"))
dados <- rbind(dados,data.frame(watts = 190, time = 1042, host = "sim-host4"))
dados <- rbind(dados,data.frame(watts = 210, time = 1042, host = "sim-host4"))
dados <- rbind(dados,data.frame(watts = 210, time = 3042, host = "sim-host4"))
dados <- rbind(dados,data.frame(watts = 100, time = 3053, host = "sim-host4"))
dados <- rbind(dados,data.frame(watts = 20, time = 3053, host = "sim-host4"))
dados <- rbind(dados,data.frame(watts = 20, time = 3053, host = "sim-host4"))

ggplot(dados, aes(x=time, y=watts, colour=host, shape=host, group=host, linetype=host)) + 
        #geom_point(size = 2.5) +
        geom_line(size=1) +
   scale_linetype_manual(values=c(1,1,1,1,2,3,4,6))+
	scale_y_continuous(limits=c(20, 300), breaks=seq(20, 300, by=25)) +
	scale_color_manual(values=c("real-host1"="#f768a1", "real-host2"="#78c679", "real-host3"="#74a9cf", "real-host4"="#fb6a4a","sim-host1"="black", "sim-host2"="black", "sim-host3"="black", "sim-host4"="black")) +
	theme_bw() +
	theme(legend.title=element_blank(), text = element_text(size=15)) +
	annotate("text", x=150, y=250, label="A", color = "black") +
	geom_vline(xintercept=300, linetype="dotted") + 
	annotate("text", x=365, y=250, label="B", color = "black") +
	geom_vline(xintercept=442, linetype="dotted") + 
	annotate("text", x=1500, y=250, label="C", color = "black") +
	geom_vline(xintercept=2443, linetype="dotted") + 
	annotate("text", x=2743, y=250, label="D", color = "black") +

	#geom_vline(xintercept=1620, linetype="dotted") + 
	#geom_vline(xintercept=1920, linetype="dotted") + 
	#geom_vline(xintercept=2220, linetype="dotted") + 
	xlab("Execution Time (seconds)") +
	ylab("Energy Consumption (Wh)")
     
dev.off()

########################################################################
## Dados reais - standby
########################################################################

postscript("results-standby-linhas-real.eps", horizontal = FALSE, onefile = FALSE, paper = "special", width = 7, height = 5)
dados<-read.table("result.sleepstate.real.standby.txt", header=TRUE, sep=";")

#dfc <- summarySE(dados, measurevar="watts", groupvars=c("host", "time"))

dados <- rbind(dados,data.frame(watts = 120, time = 0, host = "sim-host1"))
dados <- rbind(dados,data.frame(watts = 150, time = 0, host = "sim-host1"))
dados <- rbind(dados,data.frame(watts = 150, time = 7, host = "sim-host1"))
dados <- rbind(dados,data.frame(watts = 210, time = 7, host = "sim-host1"))
dados <- rbind(dados,data.frame(watts = 210, time = 2007, host = "sim-host1"))
dados <- rbind(dados,data.frame(watts = 150, time = 2007, host = "sim-host1"))
dados <- rbind(dados,data.frame(watts = 150, time = 2016, host = "sim-host1"))
dados <- rbind(dados,data.frame(watts = 120, time = 2016, host = "sim-host1"))
dados <- rbind(dados,data.frame(watts = 120, time = 2916, host = "sim-host1"))
 
dados <- rbind(dados,data.frame(watts = 120, time = 0, host = "sim-host2"))
dados <- rbind(dados,data.frame(watts = 120, time = 300, host = "sim-host2"))
dados <- rbind(dados,data.frame(watts = 150, time = 300, host = "sim-host2"))
dados <- rbind(dados,data.frame(watts = 150, time = 307, host = "sim-host2"))
dados <- rbind(dados,data.frame(watts = 210, time = 307, host = "sim-host2"))
dados <- rbind(dados,data.frame(watts = 210, time = 2307, host = "sim-host2"))
dados <- rbind(dados,data.frame(watts = 150, time = 2307, host = "sim-host2"))
dados <- rbind(dados,data.frame(watts = 150, time = 2316, host = "sim-host2"))
dados <- rbind(dados,data.frame(watts = 120, time = 2316, host = "sim-host2"))
dados <- rbind(dados,data.frame(watts = 120, time = 2916, host = "sim-host2"))

dados <- rbind(dados,data.frame(watts = 120, time = 0, host = "sim-host3"))
dados <- rbind(dados,data.frame(watts = 120, time = 600, host = "sim-host3"))
dados <- rbind(dados,data.frame(watts = 150, time = 600, host = "sim-host3"))
dados <- rbind(dados,data.frame(watts = 150, time = 607, host = "sim-host3"))
dados <- rbind(dados,data.frame(watts = 210, time = 607, host = "sim-host3"))
dados <- rbind(dados,data.frame(watts = 210, time = 2607, host = "sim-host3"))
dados <- rbind(dados,data.frame(watts = 150, time = 2607, host = "sim-host3"))
dados <- rbind(dados,data.frame(watts = 150, time = 2616, host = "sim-host3"))
dados <- rbind(dados,data.frame(watts = 120, time = 2616, host = "sim-host3"))
dados <- rbind(dados,data.frame(watts = 120, time = 2916, host = "sim-host3"))

dados <- rbind(dados,data.frame(watts = 120, time = 0, host = "sim-host4"))
dados <- rbind(dados,data.frame(watts = 120, time = 900, host = "sim-host4"))
dados <- rbind(dados,data.frame(watts = 150, time = 900, host = "sim-host4"))
dados <- rbind(dados,data.frame(watts = 150, time = 907, host = "sim-host4"))
dados <- rbind(dados,data.frame(watts = 210, time = 907, host = "sim-host4"))
dados <- rbind(dados,data.frame(watts = 210, time = 2907, host = "sim-host4"))
dados <- rbind(dados,data.frame(watts = 150, time = 2907, host = "sim-host4"))
dados <- rbind(dados,data.frame(watts = 150, time = 2916, host = "sim-host4"))
dados <- rbind(dados,data.frame(watts = 120, time = 2916, host = "sim-host4"))
dados <- rbind(dados,data.frame(watts = 120, time = 2916, host = "sim-host4"))


ggplot(dados, aes(x=time, y=watts, colour=host, shape=host, group=host, linetype=host)) + 
        #geom_point(size = 2.5) +
	geom_line(size=1) +
    scale_linetype_manual(values=c(1,1,1,1,2,3,4,6))+
	scale_y_continuous(limits=c(20, 300), breaks=seq(20, 300, by=50)) +
	#scale_x_continuous(limits=c(20, 200), breaks=seq(20, 200, by=10)) +
	scale_color_manual(values=c("real-host1"="#f768a1", "real-host2"="#78c679", "real-host3"="#74a9cf", "real-host4"="#fb6a4a","sim-host1"="black", "sim-host2"="black", "sim-host3"="black", "sim-host4"="black")) +
	theme_bw() +
	theme(legend.title=element_blank(), text = element_text(size=15)) +
	#annotate("text", x=150, y=250, label="A", color = "black") +
	#geom_vline(xintercept=300, linetype="dotted") + 
	#annotate("text", x=355, y=250, label="B", color = "black") +
	#geom_vline(xintercept=420, linetype="dotted") + 
	#annotate("text", x=940, y=250, label="C", color = "black") +
	#geom_vline(xintercept=1500, linetype="dotted") + 
	#annotate("text", x=1800, y=250, label="D", color = "black") +

	#geom_vline(xintercept=1620, linetype="dotted") + 
	#geom_vline(xintercept=1920, linetype="dotted") + 
	#geom_vline(xintercept=2220, linetype="dotted") + 
	xlab("Execution Time (seconds)") +
	ylab("Energy Consumption (Wh)")
     
dev.off()

########################################################################
## Dados reais - All included
########################################################################


postscript("results-standby-linhas-real.eps", horizontal = FALSE, onefile = FALSE, paper = "special", width = 7, height = 5)
dados<-read.table("result.sleepstate.real.states.txt", header=TRUE, sep=";")

#dfc <- summarySE(dados, measurevar="watts", groupvars=c("state"))

ggplot(dados, aes(x=time, y=watts, colour=host, shape=host, groupvars=c("state", "host"))) + 
        #geom_point(size = 2.5) +
        geom_line() +
	#scale_y_continuous(limits=c(20, 300), breaks=seq(20, 300, by=50)) +
	#scale_x_continuous(limits=c(20, 200), breaks=seq(20, 200, by=10)) +
	theme_bw() +
	scale_y_continuous(limits=c(20, 300), breaks=seq(20, 300, by=50)) +
	#theme(legend.title=element_blank(), text = element_text(size=15)) +
	#annotate("text", x=150, y=250, label="A", color = "black") +
	#geom_vline(xintercept=300, linetype="dotted") + 
	#annotate("text", x=355, y=250, label="B", color = "black") +
	#geom_vline(xintercept=420, linetype="dotted") + 
	#annotate("text", x=940, y=250, label="C", color = "black") +
	#geom_vline(xintercept=1500, linetype="dotted") + 
	#annotate("text", x=1800, y=250, label="D", color = "black") +

	#geom_vline(xintercept=1620, linetype="dotted") + 
	#geom_vline(xintercept=1920, linetype="dotted") + 
	#geom_vline(xintercept=2220, linetype="dotted") + 
	facet_wrap(~host,nrow=3)  
	#xlab("Execution Time (seconds)") +
	#ylab("Energy Consumption (watts)")
     
dev.off()

########################################################################
## Grafico de barras (workflow)
########################################################################

df <- data.frame(Events = c("Cloudlet 1", "Cloudlet 2", "Cloudlet 3", "Cloudlet 4"), 
		 x_max =  c(1320, 1620, 1920, 2220), 
		 x_min =  c(0, 300, 600, 900), 
		 height = c(20, 40, 60, 80))

ggplot(df, aes(ymin = 0)) + scale_y_continuous(limits=c(0, 100), breaks=seq(0, 100, by=20)) + 
	theme_bw() + 
	geom_rect(aes(xmin = x_min, xmax = x_max, ymax = height, fill = Events, alpha=0.5)) + 
	scale_colour_hue(legend=FALSE) + 
	scale_alpha(guide = 'none')

ggsave(file="results-workflow.png")

