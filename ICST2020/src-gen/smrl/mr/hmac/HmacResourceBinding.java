package smrl.mr.hmac;

import java.util.List;
import smrl.mr.language.Action;
import smrl.mr.language.Operations;

import smrl.mr.language.MR;

@SuppressWarnings("all")
public class HmacResourceBinding extends MR{
  public boolean mr() {
    List<Action> _actions = Operations.Input(1).actions();
    for (final Action action : _actions) {
      for (int par = 0; (par < action.getParameters().size()); par++) {
        {
          int pos = action.getPosition();
          {
            ifThenBlock();
            if (((((action.getParameterName(par).equals("file") && action.getParameterValue(par).equals("A")) && (!Operations.Output(Operations.Input(1), pos).isError())) && Operations.EQUAL(Operations.Input(2), Operations.Input(1))) && Operations.Input(2).actions().get(pos).setParameterValue(par, "B"))) {
              ifThenBlock();
              boolean _isError = Operations.Output(Operations.Input(2), pos).isError();
              if (_isError) {
                expressionPass(); /* //PROPERTY HOLDS" */
              } else {
                return Boolean.valueOf(false);
              }
            } else {
              expressionPass(); /* //PROPERTY HOLDS" */
            }
          }
        }
      }
    }
    return true;
  }
}
