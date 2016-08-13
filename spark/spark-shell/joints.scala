/**explore cloudera provided DataCo dataset in mysql
 * mysql -u retail_dba -p cloudera
 * use retail_db;
 * show tables;
 * +---------------------+
 * | Tables_in_retail_db |
 * +---------------------+
 * | categories          |
 * | customers           |
 * | departments         |
 * | order_items         |
 * | orders              |
 * | products            |
 * +---------------------+
 * 
 * desc orders;
 * +-------------------+-------------+------+-----+---------+----------------+
 * | Field             | Type        | Null | Key | Default | Extra          |
 * +-------------------+-------------+------+-----+---------+----------------+
 * | order_id          | int(11)     | NO   | PRI | NULL    | auto_increment |
 * | order_date        | datetime    | NO   |     | NULL    |                |
 * | order_customer_id | int(11)     | NO   |     | NULL    |                |
 * | order_status      | varchar(45) | NO   |     | NULL    |                |
 * +-------------------+-------------+------+-----+---------+----------------+
 * 
 * desc order_items;
 * +--------------------------+------------+------+-----+---------+----------------+
 * | Field                    | Type       | Null | Key | Default | Extra          |
 * +--------------------------+------------+------+-----+---------+----------------+
 * | order_item_id            | int(11)    | NO   | PRI | NULL    | auto_increment |
 * | order_item_order_id      | int(11)    | NO   |     | NULL    |                |
 * | order_item_product_id    | int(11)    | NO   |     | NULL    |                |
 * | order_item_quantity      | tinyint(4) | NO   |     | NULL    |                |
 * | order_item_subtotal      | float      | NO   |     | NULL    |                |
 * | order_item_product_price | float      | NO   |     | NULL    |                |
 * +--------------------------+------------+------+-----+---------+----------------+
 * 
 * join orders and order_items, get order count and revenue per day
 * orders and order_items dataset in hdfs
 * hdfs dfs -ls sqoop_import
 */
 /*** spark shell ***/
 val ordersRDD = sc.textFile("sqoop_import/orders")
 val orderItemsRDD = sc.textFile("sqoop_import/order_items")
 
 ordersRDD.take(5).foreach(println)
 orderItemsRDD.take(5).foreach(println)
 
 val ordersOIDRDD = ordersRDD.map(r => (r.split(",")(0).toLong,r))
 val orderItemsOIDRDD = orderItemsRDD.map(r => (r.split(",")(1).toLong,r))
 
 ordersOIDRDD.take(5).foreach(println)
 orderItemsOIDRDD.take(5).foreach(println)
 
 val order_orderItmRDD = orderItemsOIDRDD.join(ordersOIDRDD)
 order_orderItmRDD.take(5).foreach(println)
 
 val date_odrID_subttlRDD = order_orderItmRDD.
                            map(r => (r._2._2.split(",")(1)+"|"+r._1.toString,r._2._1.split(",")(4).toFloat)).
                            reduceByKey((x,y) => x+y)
 date_odrID_subttlRDD.take(5).foreach(println)
 
 val date_odrCnt_RevenueRDD = date_odrID_subttlRDD.
                              map(r => (r._1.split('|')(0),(1,r._2))).
                              reduceByKey((x,y) => (x._1+y._1,x._2+y._2))
 date_odrCnt_RevenueRDD.sortByKey().take(5).foreach(println)
 /** output
  * (2013-07-25 00:00:00.0,(116,68153.84))
  * (2013-07-26 00:00:00.0,(233,136520.17))
  * (2013-07-27 00:00:00.0,(175,101074.336))
  * (2013-07-28 00:00:00.0,(158,87123.09))
  * (2013-07-29 00:00:00.0,(216,137287.1))
  */
 date_odrCnt_RevenueRDD.sortByKey().collect.foreach(println)
 
/*** hive context ***/

 import org.apache.spark.sql.hive.HiveContext
 val hc = new HiveContext(sc)
 hc.sql("set spark.sql.shuffle.partitions=10")
 
 hc.sql("select * from orders limit 5").collect.foreach(println)
 hc.sql("select * from order_items limit 5").collect.foreach(println)
 
 val orderCntTotalRDD = hc.sql("""select o.order_date, 
 cast(count(distinct o.order_id) as int) order_count,
 sum(oi.order_item_subtotal) order_total_amount 
 from orders o, order_items oi 
 where o.order_id=oi.order_item_order_id 
 group by o.order_date
 order by o.order_date""") 
 
 orderCntTotalRDD.take(5).foreach(println)
 /**output
  * [2013-07-25 00:00:00.0,116,68153.82999999999]
  * [2013-07-26 00:00:00.0,233,136520.17000000025]
  * [2013-07-27 00:00:00.0,175,101074.34000000008]
  * [2013-07-28 00:00:00.0,158,87123.08000000007]
  * [2013-07-29 00:00:00.0,216,137287.09000000026]
  */
 
 
 
 
