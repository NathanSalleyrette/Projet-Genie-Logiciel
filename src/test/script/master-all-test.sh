#! /bin/sh

# Auteur : gl01
# Version initiale : 01/01/2021

# Lance tout les tests
# Commentez / retirez du tableau "tests" les tests qui ne passent pas
tests=("./src/test/script/script_test_general.sh test_lex ./src/test/deca/lexer" "./src/test/script/script_test_general.sh test_synt ./src/test/deca/syntax" "./src/test/script/script_test_general.sh test_context ./src/test/deca/context" "./src/test/script/script_test_general.sh src/test/script/launchers/decac_plus_ima.sh ./src/test/deca/codegen/" "./src/test/script/test_decompile.sh")
tests+=("./src/test/script/launchers/decac_r.sh ./src/test/deca/codegen/valid/created/while.deca")
test_echoue=false

for test in "${tests[@]}"
do
  echo $test
  $test
  if [ $? != 0 ];then
    echo "un ou plusieurs test ont echoues"
    test_echoue=true # pour faire passer # test_echoue = true
  fi
done



if [ "$test_echoue" = true ]; then
	exit 1
fi
exit 0
