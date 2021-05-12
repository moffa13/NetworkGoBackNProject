package reso.examples.gobackn;

public class RenoCC {
	
	private int _cwnd = 1;
	private boolean _slowStart = true;
	private int _lastACKSqNb = -1;
	private int _repeatedACK = 1;
	private static final int MAX_DUP_ACK = 3;
	private static final int DUP_ACK_CWND_DIVIDE = 2;
	private int _ssthresh = 20;
	
	public RenoCC(){

	}
	
	public void timeout(){
		_cwnd = 1;
		_slowStart = true;
	}

	public void receiveACK(int sqNb) {
		if(sqNb == _lastACKSqNb){ // Duplicate ACK
			_lastACKSqNb++;
			if(_lastACKSqNb == MAX_DUP_ACK){
				_cwnd /= DUP_ACK_CWND_DIVIDE;
				if(_cwnd == 0) _cwnd = 1;
			}
		}else{ // Different ACK
			_lastACKSqNb = sqNb;
			_lastACKSqNb = 1;
			
			if(_slowStart){
				_cwnd++;
			}
			
		}
	}
}
