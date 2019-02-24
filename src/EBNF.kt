import java.util.*

data class Rule(val lhs: Identifier, val rhs: RHS)
abstract class RHS
data class Terminal(val letter: Char): RHS()
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
        if(terminal.length == 1 && isCharacter(terminal[0])){
            return Terminal(terminal[0])
        }
    }

    return null
}

fun checkLHS(s: String) : Identifier? {
    val identifier =  checkIdentifier(s)

    if(identifier != null){
        return identifier
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

        val firstOrPos = indexOfFirst(s, '|', Pair('(', ')'), Pair('{', '}'))
        val firstConcatPos = indexOfFirst(s, ',', Pair('(', ')'), Pair('{', '}'))

        if(firstOrPos >= 0 && (firstOrPos < firstConcatPos || firstConcatPos < 0)){
            /**Checa se é um Ou**/
            val left = checkRHS(s.substring(0, firstOrPos))
            if(left != null){
                val right = checkRHS(s.substring(firstOrPos + 1))
                if(right != null){
                    return Or(left, right)
                }
            }
        }else if(firstConcatPos >= 0){
            /**Checa se é uma Concatenação**/
            val left = checkRHS(s.substring(0, firstConcatPos))
            if(left != null){
                val right = checkRHS(s.substring(firstConcatPos + 1))
                if(right != null){
                    return Concat(left, right)
                }
            }
        }
    }

    return null
}

fun indexOfFirst(string: String, char: Char, vararg groupIgnore: Pair<Char, Char>) : Int {

    val map = hashMapOf<Char, Char>()

    for(par in groupIgnore){
        map[par.first] = par.second
    }

    val pilha = Stack<Char>()

    for(index in 0 until string.length) {
        val charAtual = string[index]

        if (pilha.isEmpty() && charAtual == char) {
            return index
        } else if (map.containsKey(charAtual)) {
            pilha.push(charAtual)
        } else if (pilha.isNotEmpty() && charAtual == map[pilha.peek()]) {
            pilha.pop()
        }
    }

    return -1
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