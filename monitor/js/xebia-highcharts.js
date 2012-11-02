 var countChart, hitChart;

        $(document).ready(function() {
        Highcharts.setOptions({
        global: {
        useUTC: false
        }
        });

        countChart = new Highcharts.Chart({
        chart: {
        renderTo: 'countContainer',
        type: 'spline',
        marginRight: 10,
        events: {
        load: function() {
                // load is done inside index.html javascript
            }
        }
        },
        title: {
        text: 'cache size'
        },
        xAxis: {
        type: 'datetime',
        tickPixelInterval: 150
        },
        yAxis: {
        title: {
        text: '# of elements'
        },
        plotLines: [{
        value: 0,
        width: 1,
        color: '#808080'
        }]
        },
        plotOptions: {
            spline: {
                marker: {enabled: false}
            }
        },
        tooltip: {
        formatter: function() {
        return '<b>'+ this.series.name +'</b><br/>'+
        Highcharts.dateFormat('%Y-%m-%d %H:%M:%S', this.x) +'<br/>'+
        Highcharts.numberFormat(this.y, 2);
        }
        },
        legend: {
        enabled: false
        },
        exporting: {
        enabled: false
        },
        series: [{
        name: '# of element',
        data: (function() {
        // generate an array of random data
        var data = [],
        time = (new Date()).getTime();
        return data;
        })()
        }]
        });




                });