package com.example.lk_etch_robot.util

object ByteDataChange {
    fun ByteToString(bytes: ByteArray): String {
        val hexStringBuffer = StringBuilder()
        for (b in bytes) {
            val hexString = String.format("%02X", b)
            // 将转换后的十六进制字符串添加到字符串缓冲区中
            hexStringBuffer.append(hexString)
        }
        return hexStringBuffer.toString();
    }

    /**
     * 校验
     */
    open fun HexStringToBytes(hexString: String?): String? {
        var hexString = hexString
        if (hexString == null || hexString == "") {
            return null
        }
        hexString = hexString.trim { it <= ' ' }
        hexString = hexString.uppercase()
        val length = hexString.length / 2
        var ad = 0
        for (i in 0 until length) {
            val pos = i * 2
            ad += Integer.valueOf(hexString.substring(pos,pos+2),16)
        }
        var checkData = Integer.toHexString(ad)
        if (checkData.length > 2) {
            checkData = checkData.substring(checkData.length - 2, checkData.length)
        }
        if (checkData.length == 1) {
            checkData = "0$checkData"
        }
        return checkData.uppercase()
    }
}