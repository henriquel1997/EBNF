data class Rule(val lhs: LHS, val rhs: RHS)
data class LHS(val identifier: Identifier)
abstract class RHS
data class Terminal(val name: String): RHS()
data class Identifier(val name: String) : RHS()
data class Loop(val item: RHS) : RHS()
data class Or(val left: RHS, val right: RHS) : RHS()
data class Concat(val left: RHS, val right: RHS) : RHS()

fun isLetter(c : Char) : Boolean {
    return  ((c in 'a'..'z') || (c in 'A'..'Z'))
}

fun isDigit(c: Char) : Boolean {
    return (c in '0'..'9')
}

fun isCharacter(c: Char) : Boolean {
    return isLetter(c) || isDigit(c) || c == '_'
}

fun checkIdentifier(s: String) :  Identifier? {
    if(isLetter(s[0])){
        if(s.length > 1){
            for(i in 1 until s.length){
                if(!isDigit(s[i]) && !isLetter(s[i]) && s[i] != '_'){
                    return null
                }
            }
        }

        return Identifier(s)
    }

    return null
}

fun checkTerminal(s: String) : Terminal? {
    if(s[0] == '\'' && s[s.length - 1] == '\'' ||
        s[0] == '"' && s[s.length - 1] == '"'){
        val terminal = s.dropLast(1).drop(1)
        if(isCharacter(terminal[0])){
            if(terminal.length > 1) {
                for (i in 1 until terminal.length) {
                    if (!isCharacter(terminal[i])) {
                        return null
                    }
                }
            }

            return Terminal(terminal)
        }
    }

    return null
}

fun checkLHS(s: String) : LHS? {
    val identifier =  checkIdentifier(s)

    if(identifier != null){
        return LHS(identifier)
    }

    return null
}

fun checkRHS(s: String) : RHS? {

    if(s.isNotEmpty()){
        /**Checa se é um Terminal**/
        var rhs:RHS? = checkTerminal(s)
        if(rhs != null){
            return rhs
        }

        /**Checa se é um Identificador**/
        rhs = checkIdentifier(s)
        if(rhs != null){
            return rhs
        }

        if(s[0] == '{' && s[s.length - 1] == '}'){
            /**Checa se é um Loop**/
            rhs = checkRHS(s.dropLast(1).drop(1))
            if(rhs != null){
                return Loop(rhs)
            }
        } else if(s[0] == '(' && s[s.length - 1] == ')'){
            /**Checa se é um Agrupamento**/
            rhs = checkRHS(s.dropLast(1).drop(1))
            if(rhs != null){
                return rhs
            }
        }

        /**Checa se é um Ou**/
        var split = s.split('|', limit = 2)
        if(split.size == 2){
            val left = checkRHS(split[0])
            if(left != null){
                val right = checkRHS(split[1])
                if(right != null){
                    return Or(left, right)
                }
            }
        } else {

            /**Checa se é uma Concatenação**/
            split = s.split(',', limit = 2)
            if(split.size == 2){
                val left = checkRHS(split[0])
                if(left != null){
                    val right = checkRHS(split[1])
                    if(right != null){
                        return Concat(left, right)
                    }
                }
            }
        }
    }

    return null
}

fun checkRule(s: String) : Rule? {

    if(s[s.length -1] == ';'){
        val split = s.dropLast(1).split('=', limit = 2)
        if(split.size == 2){
            val lhs = checkLHS(split[0])
            if(lhs != null){
                val rhs = checkRHS(split[1])
                if(rhs != null){
                    return Rule(lhs, rhs)
                }
            }
        }
    }

    return null
}