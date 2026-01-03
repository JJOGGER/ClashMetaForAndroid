package com.xboard.util

import com.xboard.R

/**
 * 国家图标映射工具类
 * 根据节点名称中的国家关键词，返回对应的图标资源ID
 */
object CountryIconMapper {
    
    /**
     * 根据节点名称获取国家图标资源ID
     * 
     * @param nodeName 节点名称，可能包含国家关键词
     * @return 对应的图标资源ID，如果未匹配到则返回 ico_unknown
     */
    @JvmStatic
    fun getCountryIconResId(nodeName: String?): Int {
        if (nodeName.isNullOrBlank()) {
            return R.drawable.ico_unknown
        }
        
        val name = nodeName.lowercase()
        
        // 按优先级匹配（更具体的匹配放在前面）
        return when {
            // 亚洲
            name.contains("香港") || name.contains("hongkong") || name.contains("hk") -> R.drawable.ico_hk
            name.contains("台湾") || name.contains("taiwan") || name.contains("tw") -> R.drawable.ico_taiwan
            name.contains("日本") || name.contains("japan") || name.contains("jp") -> R.drawable.ico_jp
            name.contains("韩国") || name.contains("korea") || name.contains("kr") -> R.drawable.ico_ko
            name.contains("新加坡") || name.contains("singapore") || name.contains("sg") -> R.drawable.ico_sg
            name.contains("马来西亚") || name.contains("malaysia") || name.contains("my") -> R.drawable.ico_malaixiya
            name.contains("印度") || name.contains("india") || name.contains("in") -> R.drawable.ico_yindu
            name.contains("泰国") || name.contains("thailand") || name.contains("th") -> R.drawable.ico_taiguo
            name.contains("越南") || name.contains("vietnam") || name.contains("vn") -> R.drawable.ico_yuenan
            name.contains("菲律宾") || name.contains("philippines") || name.contains("ph") -> R.drawable.ico_feilibin
            name.contains("印度尼西亚") || name.contains("indonesia") || name.contains("id") -> R.drawable.ico_yindunixiya
            
            // 欧洲
            name.contains("英国") || name.contains("united kingdom") || name.contains("uk") || name.contains("gb") -> R.drawable.ico_yingguo
            name.contains("德国") || name.contains("germany") || name.contains("de") -> R.drawable.ico_deguo
            name.contains("法国") || name.contains("france") || name.contains("fr") -> R.drawable.ico_faguo
            name.contains("荷兰") || name.contains("netherlands") || name.contains("nl") -> R.drawable.ico_helan
            name.contains("瑞士") || name.contains("switzerland") || name.contains("ch") -> R.drawable.ico_ruishi
            name.contains("瑞典") || name.contains("sweden") || name.contains("se") -> R.drawable.ico_ruidian
            name.contains("挪威") || name.contains("norway") || name.contains("no") -> R.drawable.ico_nuowei
            name.contains("丹麦") || name.contains("denmark") || name.contains("dk") -> R.drawable.ico_danmai
            name.contains("芬兰") || name.contains("finland") || name.contains("fi") -> R.drawable.ico_fenlan
            name.contains("西班牙") || name.contains("spain") || name.contains("es") -> R.drawable.ico_xibanya
            name.contains("意大利") || name.contains("italy") || name.contains("it") -> R.drawable.ico_yidali
            name.contains("葡萄牙") || name.contains("portugal") || name.contains("pt") -> R.drawable.ico_putaoya
            name.contains("比利时") || name.contains("belgium") || name.contains("be") -> R.drawable.ico_bilishi
            name.contains("奥地利") || name.contains("austria") || name.contains("at") -> R.drawable.ico_aodili
            name.contains("波兰") || name.contains("poland") || name.contains("pl") -> R.drawable.ico_bolan
            name.contains("捷克") || name.contains("czech") || name.contains("cz") -> R.drawable.ico_jieke
            name.contains("匈牙利") || name.contains("hungary") || name.contains("hu") -> R.drawable.ico_xiongyali
            name.contains("爱尔兰") || name.contains("ireland") || name.contains("ie") -> R.drawable.ico_aierlan
            name.contains("俄罗斯") || name.contains("russia") || name.contains("ru") -> R.drawable.ico_eluosi
            name.contains("乌克兰") || name.contains("ukraine") || name.contains("ua") -> R.drawable.ico_wukelan
            name.contains("土耳其") || name.contains("turkey") || name.contains("tr") -> R.drawable.ico_tuerqi
            
            // 北美洲
            name.contains("美国") || name.contains("united states") || name.contains("usa") || name.contains("us") -> R.drawable.ico_us
            name.contains("加拿大") || name.contains("canada") || name.contains("ca") -> R.drawable.ico_jianada
            name.contains("墨西哥") || name.contains("mexico") || name.contains("mx") -> R.drawable.ico_moxige
            
            // 南美洲
            name.contains("巴西") || name.contains("brazil") || name.contains("br") -> R.drawable.ico_baxi
            name.contains("阿根廷") || name.contains("argentina") || name.contains("ar") -> R.drawable.ico_agenting
            name.contains("哥伦比亚") || name.contains("colombia") || name.contains("co") -> R.drawable.ico_gelunbiya
            name.contains("智利") || name.contains("chile") || name.contains("cl") -> R.drawable.ico_zhili
            
            // 大洋洲
            name.contains("澳大利亚") || name.contains("australia") || name.contains("au") -> R.drawable.ico_aodaliya
            name.contains("新西兰") || name.contains("new zealand") || name.contains("nz") -> R.drawable.ico_xinxinan
            
            // 非洲
            name.contains("南非") || name.contains("south africa") || name.contains("za") -> R.drawable.ico_nanfei
            name.contains("埃及") || name.contains("egypt") || name.contains("eg") -> R.drawable.ico_aiji
            
            // 中东
            name.contains("阿联酋") || name.contains("uae") || name.contains("united arab emirates") || name.contains("ae") -> R.drawable.ico_alianqiu
            name.contains("沙特阿拉伯") || name.contains("saudi arabia") || name.contains("sa") -> R.drawable.ico_shate
            name.contains("以色列") || name.contains("israel") || name.contains("il") -> R.drawable.ico_yiselie
            name.contains("卡塔尔") || name.contains("qatar") || name.contains("qa") -> R.drawable.ico_kataer

            // 默认未知
            else -> R.drawable.ico_unknown
        }
    }
}

