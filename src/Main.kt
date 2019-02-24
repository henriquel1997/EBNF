fun main() {

    val ruleString = "letter = {\"A\" | \"B\"};"
    val beginTime = System.currentTimeMillis()
    val rule = checkRule(ruleString.replace("\\s".toRegex(), ""))
    val automato = if(rule != null){
        val a = convertRHSToAutomato(rule.rhs)
        convertNFAtoDFA(a)
    } else {
        null
    }

    val endTime = System.currentTimeMillis()

    println("$ruleString : is rule: $rule")
    println("Tempo: "+ (endTime - beginTime) + "ms")
}