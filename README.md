# BigcoPricecheck
#### v.1.0.1

## Background
This problem statement is contained in instructions.txt, and indicates the developer will be running the application at the request of BIGCO accountants, so an interface that would be accessible to an experienced software developer was the design objective for the resulting application.

## Building
If you're reading this file, you've already unpacked the tarball provided with the source code produced for the reporting task, as well as the data files as provided by BIGCO personnel for testing.

The suggested steps for building the application assume that your shell is in the same directory as this README file.
```
lein deps
lein uberjar
```

## Running
Pricecheck takes three options on the commandline, **each of which is required** on every invocation of the application. 
```
-p path/to/price/datafile.csv    # The name (including relative or absolute path if 
                                 # not in current directory) of  the file containing 
                                 # lookup  prices when crosschecking sale  prices on 
                                 # receipts
                                 
-d path/to/data/directory        # The path (relative or absolute) to the directory 
                                 # containing  receipt data files. All sub-directories 
                                 # in the directory  will be included in the price
                                 # crosscheck data analyzed for the report

-o path/to/reportfile.csv        # The path (relative or absolute) to the desired 
                                 # location for the saved report for Pricecheck.
                                 # If the file specified here already exists, the 
                                 # application will abort. You can either provide 
                                 # a different name, or delete or move the existing
                                 # report file, and then re-run.

```

If you wish to evaluate and report on the dataset provided by BIGCO for this exercise, which is contained in the `data` subdirectory, execute the `tar` command and then you should be able to use the following sample invocation to do so.

```
tar zxf data.tar.gz

java -jar target/uberjar/bigco-pricecheck-1.0.1-standalone.jar -d data \
-p data/prices.csv -o price-errors.csv
```

To view the resulting report contents:
```
cat price-errors.csv
```
This report should look very similar to:
```
store,plusminus
1,-5598.99
5,-2421.11
4,-794.92
2,+2198.09
3,+2920.78
```
Your plusminus numbers will vary if using a different dataset, and you may have different stores as part of your analysis set, as well.
