package com.tbread.entity

enum class JobClass(val className: String, val basicSkillCode: Int) {
    GLADIATOR("gladiator", 11020000),
    TEMPLAR("templar", 12010000),
    RANGER("ranger", 14020000),
    ASSASSIN("assassin", 13010000),
    SORCERER("sorcerer", 15210000), /* 마도 확인 필요함 */
    CLERIC("cleric", 17010000),
    ELEMENTALIST("elementalist", 16010000),
    CHANTER("chanter", 18010000);

    companion object{
        fun convertFromSkill(skillCode:Int):JobClass?{
            return entries.find { it.basicSkillCode == skillCode }
        }
    }
}