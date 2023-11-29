package com.example.lk_etch_robot.util

import kotlin.experimental.or

object BinaryChange {
    fun tenToHex(data:Int) = Integer.toHexString(data)!!

    /**
     * hexString 转byte
     */
    fun hexStringToByte(data:String): Array<String> {
        check(data.length % 2 == 0) { "Must have an even length" }
        val byteIterator = data.chunkedSequence(2)
            .map {it}
            .iterator()
        return Array(data.length / 2) { byteIterator.next() }
    }

    /**
     * 将16进制字符串转换为byte[]
     * @param str
     * @return
     */
    fun toBytes(str: String?): ByteArray? {
        if (str == null || str.trim { it <= ' ' } == "") {
            return ByteArray(0)
        }
        val bytes = ByteArray(str.length / 2)
        for (i in 0 until str.length / 2) {
            val subStr = str.substring(i * 2, i * 2 + 2)
            bytes[i] = subStr.toInt(16).toByte()
        }
        return bytes
    }

    /**
     * 设置数据格式
     */
    fun dataChange(hex: String, division: Boolean): String? {
        val stringData: String = if (division) {
            String.format("%.1f", hex.toInt(16).toFloat() / 10)
        } else {
            String.format("%.0f", hex.toBigInteger(16).toFloat())
        }
        return stringData
    }


    /**
     * iEEE754转float
     */
    fun ieee754ToFloat(ieeData: Int): Float {
        return java.lang.Float.intBitsToFloat(ieeData)
    }

    fun ieee754ToFloat(ieeData: Long): Float {
        return java.lang.Float.intBitsToFloat(ieeData.toInt())
    }

    /**
     * IEEE 754字符串转十六进制字符串
     *
     * @param f
     * @author: 若非
     * @date: 2021/9/10 16:57
     */
    fun singleToHex(f: Float): String? {
        val i = java.lang.Float.floatToIntBits(f)
        return Integer.toHexString(i)
    }

    fun float2byte(f: Float): ByteArray? {
        // 把float转换为byte[]
        val fbit = java.lang.Float.floatToIntBits(f)
        val b = ByteArray(4)
        for (i in 0..3) {
            b[i] = (fbit shr 24 - i * 8).toByte()
        }

        // 翻转数组
        val len = b.size
        // 建立一个与源数组元素类型相同的数组
        val dest = ByteArray(len)
        // 为了防止修改源数组，将源数组拷贝一份副本
        System.arraycopy(b, 0, dest, 0, len)
        var temp: Byte
        // 将顺位第i个与倒数第i个交换
        for (i in 0 until len / 2) {
            temp = dest[i]
            dest[i] = dest[len - i - 1]
            dest[len - i - 1] = temp
        }
        return dest
    }

    private const val hexStr = "0123456789ABCDEF"
    /**
     * @param hexString
     * @return 将十六进制转换为字节数组
     */
    fun HexStringToByteArray(hexString: String): ByteArray? {
        //hexString的长度对2取整，作为bytes的长度
        val len = hexString.length / 2
        val bytes = ByteArray(len)
        var high: Byte = 0 //字节高四位
        var low: Byte = 0 //字节低四位
        for (i in 0 until len) {

            //右移四位得到高位
            high = (hexStr.indexOf(hexString[2 * i]) shl 4).toByte()
            low = hexStr.indexOf(hexString[2 * i + 1]).toByte()
            bytes[i] = (high or low) as Byte //高地位做或运算
        }
        return bytes
    }

    /**
     * 高位补零
     */
    fun addZeroForNum(str: String, strLength: Int): String? {
        var str = str
        var strLen = str.length
        if (strLen < strLength) {
            while (strLen < strLength) {
                val sb = StringBuffer()
                sb.append("0").append(str);// 左补0
//                sb.append(str).append("0") //右补0
                str = sb.toString()
                strLen = str.length
            }
        }
        str = String.format(str).toUpperCase() //转为大写
        return str
    }

    /**
     * 将16进制转换为二进制
     * @param hexStr
     * @return
     */
    fun parseHexStr2Byte(hexStr: String): ByteArray? {
        if (hexStr.length < 1) return null
        val result = ByteArray(hexStr.length / 2)
        for (i in 0 until hexStr.length / 2) {
            val high = hexStr.substring(i * 2, i * 2 + 1).toInt(16)
            val low = hexStr.substring(i * 2 + 1, i * 2 + 2).toInt(
                16
            )
            result[i] = (high * 16 + low).toByte()
        }
        return result
    }
}