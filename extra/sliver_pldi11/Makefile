default: main.pdf

texfiles := $(wildcard *.tex) $(wildcard analyze-results.out/*.tex)
figfiles := $(wildcard figures/*.pdf)

main.pdf: $(texfiles) $(figfiles) main.bbl
	@pdflatex main < /dev/null > /dev/null || true
	@grep -A 4 "Emergency stop" main.log || grep Warning main.log || true
main.bbl: main.bib
	pdflatex main
	bibtex main
	pdflatex main
	pdflatex main
main.bib: pliang.bib extra.bib
	cat pliang.bib extra.bib > main.bib

clean:
	rm -f main.aux main.bbl main.blg main.log main.pdf

run:
	go main.pdf
