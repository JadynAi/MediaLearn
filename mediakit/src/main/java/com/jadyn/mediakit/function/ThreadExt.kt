package com.jadyn.mediakit.function

/**
 *@version:
 *@FileDescription:
 *@Author:Jing
 *@Since:2019-06-30
 *@ChangeList:
 */

/**
 * 得到设备最多的线程数量
 * n为cpu核数
 * CPU 密集应用，设置为 n+1
 * IO 密集，设置为 2*n+1
 * */
fun getValidPoolSize(): Int {
    return Runtime.getRuntime().availableProcessors() * 2 + 1
}