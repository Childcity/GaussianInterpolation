package com.childcity.gaussianinterpolation.MathParser;

/**
 *
 * @author shurik
 */

import java.util.HashMap;

public class MatchParser
{
    private HashMap<String, Double> variables;

    public MatchParser()
    {
        variables = new HashMap<String, Double>();
    }

    public void setVariable(String variableName, Double variableValue)
    {
        variables.put(variableName, variableValue);
    }

    public Double getVariable(String variableName) throws Exception {
        if (!variables.containsKey(variableName)) {
            throw new Exception( "Error: Try get unexist variable '"+variableName+"'" );
            //return 0.0;
        }
        return variables.get(variableName);
    }

    public double Parse(String s) throws Exception
    {
        s = s.replaceAll("\\s", "");
        Result result = PlusMinus(s);
        if (!result.rest.isEmpty()) {
            throw new Exception("Error: can't full parse. Rest: " + result.rest);
        }
        return result.acc;
    }

    private Result PlusMinus(String s) throws Exception
    {
        Result current = MulDiv(s);
        double acc = current.acc;

        while (current.rest.length() > 0) {
            if (!(current.rest.charAt(0) == '+' || current.rest.charAt(0) == '-')) break;

            char sign = current.rest.charAt(0);
            String next = current.rest.substring(1);

            current = MulDiv(next);
            if (sign == '+') {
                acc += current.acc;
            } else {
                acc -= current.acc;
            }
        }
        return new Result(acc, current.rest);
    }

    private Result Bracket(String s) throws Exception
    {
        char zeroChar = s.charAt(0);
        if (zeroChar == '(') {
            Result r = PlusMinus(s.substring(1));
            if (!r.rest.isEmpty() && r.rest.charAt(0) == ')') {
                r.rest = r.rest.substring(1);
            } else {
                throw new Exception("Error: not close bracket");
            }
            return r;
        }
        return FunctionVariable(s);
    }

    private Result FunctionVariable(String s) throws Exception
    {
        StringBuilder f = new StringBuilder();
        int i = 0;
        // ищем название функции или переменной
        // имя обязательно должна начинаться с буквы
        while (i < s.length()
                && (Character.isLetter(s.charAt(i))
                        || (Character.isDigit(s.charAt(i)) && i > 0)
                        || s.charAt(i) == ',' // для ф-й двух  и больше переменных
                    )
        ){
            f.append(s.charAt(i));
            i++;
        }

        if (f.length() > 0) { // если что-нибудь нашли
            if (s.length() > i && s.charAt(i) == '(') { // и следующий символ скобка значит - это функция
                String fName = f.toString();
                String funcArgs = s.substring(fName.length() + 1);
                return processFunction(fName.toLowerCase(), PlusMinus(funcArgs));
            } else { // иначе - это переменная
                String variables = f.toString();
                StringBuilder variable = new StringBuilder();

                for (int j = 0; j < variables.length()
                        && (Character.isLetter(variables.charAt(j)) || Character.isDigit(variables.charAt(j))); j++)
                {
                    variable.append(variables.charAt(j));
                }
                return new Result(getVariable(variable.toString()), s.substring(variable.length()));
            }
        }

        return Num(s);
    }

    private Result MulDiv(String s) throws Exception
    {
        Result current = Bracket(s);

        double acc = current.acc;
        while (true) {
            if (current.rest.length() == 0) {
                return current;
            }
            char sign = current.rest.charAt(0);
            if ((sign != '*' && sign != '/')) return current;

            String next = current.rest.substring(1);
            Result right = Bracket(next);

            if (sign == '*') {
                acc *= right.acc;
            } else {
                acc /= right.acc;
            }

            current = new Result(acc, right.rest);
        }
    }

   /** @noinspection Duplicates*/
    private Result Num(String s) throws Exception
    {
        int i = 0;
        int dot_cnt = 0;
        boolean negative = false;
        // число также может начинаться с минуса
        if( s.charAt(0) == '-' ){
            negative = true;
            s = s.substring( 1 );
        }
        // разрешаем только цифры и точку
        while (i < s.length() && (Character.isDigit(s.charAt(i)) || s.charAt(i) == '.')) {
            // но также проверям, что в числе может быть только одна точка!
            if (s.charAt(i) == '.' && ++dot_cnt > 1) {
                throw new Exception("not valid number '" + s.substring(0, i + 1) + "'");
            }
            i++;
        }
        if( i == 0 ){ // что-либо похожее на число мы не нашли
            throw new Exception( "can't get valid number in '" + s + "'" );
        }

        double dPart = Double.parseDouble(s.substring(0, i));
        if( negative ) dPart = -dPart;
        String restPart = s.substring(i);

        return new Result(dPart, restPart);
    }

    // Тут определяем все нашие функции, которыми мы можем пользоватся в формулах
    private Result processFunction(String func, Result arg1) throws Exception
    {
        if((! arg1.rest.isEmpty()) && (arg1.rest.charAt(0) == ',')){
            Result arg2 = PlusMinus(arg1.rest.substring(1));
            if(arg2.rest.isEmpty() || arg2.rest.charAt(0) != ')'){
                throw new Exception("Error: expected ')'");
            }

            String arg2Rest = arg2.rest.substring(1);

            if (func.equals("pow")) {
                return new Result(Math.pow(arg1.acc, arg2.acc), arg2Rest);
            } else {
                throw new Exception("function with two arguments '" + func + "' is not defined");
            }
        } else {
            if(arg1.rest.isEmpty() || arg1.rest.charAt(0) != ')'){
                throw new Exception("Error: expected ')'");
            }

            String arg1Rest = arg1.rest.substring(1);

            if (func.equals("sin")) {
                return new Result(Math.sin(Math.toRadians(arg1.acc)), arg1Rest);
            } else if (func.equals("cos")) {
                return new Result(Math.cos(Math.toRadians(arg1.acc)), arg1Rest);
            } else if (func.equals("tan")) {
                return new Result(Math.tan(Math.toRadians(arg1.acc)), arg1Rest);
            } else if (func.equals("cot")) {
                return new Result(1.0 / Math.tan(Math.toRadians(arg1.acc)), arg1Rest);
            } else if (func.equals("exp")) {
                return new Result(Math.exp(arg1.acc), arg1Rest);
            } else {
                throw new Exception("function '" + func + "' with one argument '" + arg1.acc + "' is not defined. Rest '" + arg1Rest + "'");
            }
        }
    }
} 