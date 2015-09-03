# Initialiser R (nettoyer la mémoire).
rm(list = ls())
# Charger la bibliothèque ggplot.
library(ggplot2)
library(grid)
library(gridExtra)
# Fonctions.
multiplot <- function(..., plotlist=NULL, file, cols=1, layout=NULL) {
  library(grid)
  # Make a list from the ... arguments and plotlist
  plots <- c(list(...), plotlist)
  numPlots = length(plots)
  # If layout is NULL, then use 'cols' to determine layout
  if (is.null(layout)) {
    # Make the panel
    # ncol: Number of columns of plots
    # nrow: Number of rows needed, calculated from # of cols
    layout <- matrix(seq(1, cols * ceiling(numPlots/cols)),
                    ncol = cols, nrow = ceiling(numPlots/cols))
  }
 if (numPlots==1) {
    print(plots[[1]])
  } else {
    # Set up the page
    grid.newpage()
    pushViewport(viewport(layout = grid.layout(nrow(layout), ncol(layout))))
    # Make each plot, in the correct location
    for (i in 1:numPlots) {
      # Get the i,j matrix positions of the regions that contain this subplot
      matchidx <- as.data.frame(which(layout == i, arr.ind = TRUE))
      print(plots[[i]], vp = viewport(layout.pos.row = matchidx$row,
                                      layout.pos.col = matchidx$col))
    }
  }
}
makePlot <- function(datafilename, name) {
	values <- read.csv2(datafilename, header = TRUE)
	ymax <- max(values$count) + 30
	ylim <- ylim(0, ymax)
	values$event2 <- reorder(values$event, -values$count)
	p <- ggplot(values, aes(x=event2,y=count)) + geom_histogram(stat="identity") + theme(panel.grid=element_blank(), panel.background=element_blank(), axis.line=element_line(colour="black"), axis.text.x = element_text(angle = 90, hjust=1, vjust=0.5)) + xlab(name) + geom_text(aes(label=count), vjust=-1) + ylim
	return(p)
}