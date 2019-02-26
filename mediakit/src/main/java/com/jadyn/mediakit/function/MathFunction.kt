package com.jadyn.mediakit.function

/**
 *@version:
 *@FileDescription:
 *@Author:jing
 *@Since:2019/2/22
 *@ChangeList:
 */
/*
* 选取两个值中，差值绝对值小的那个数字
* */
fun Long.minDifferenceValue(a: Long, b: Long): Long {
    if (a == b) {
        return a
    }
    val f_a = Math.abs(a - this)
    val f_b = Math.abs(b - this)
    if (f_a == f_b) {
        return Math.min(a, b)
    }
    return if (f_a < f_b) a else b
}