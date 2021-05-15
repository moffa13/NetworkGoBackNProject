package reso.examples.gobackn;

public class RenoCC {
	
	private int _cwnd = 1;
	private boolean _slowStart = true;
	private int _lastACKSqNb = -1;
	private int _repeatedACK = 1;
	private static final int MAX_DUP_ACK = 3;
	private static final int DUP_ACK_CWND_DIVIDE = 2;
	private int _ssthresh = 100;
	
	public RenoCC(){

	}
	
	public void timeout(){
		_cwnd = 1;
		_slowStart = true;
	}
	
	public int getWindowSize(){
		return _cwnd;
	}

	public void receiveACK(int sqNb) {
		if(sqNb == _lastACKSqNb){ // Duplicate ACK
			_repeatedACK++;
			if(_repeatedACK == MAX_DUP_ACK){ // 3 duplicate ACK
				_cwnd /= DUP_ACK_CWND_DIVIDE; // Divide cwnd by 2
				if(_cwnd == 0) _cwnd = 1;
				_ssthresh = _cwnd; // threshold equals to half of the congestion window when loss occurs.
			}
		}else{ // Different ACK
			_lastACKSqNb = sqNb;
			_repeatedACK = 1;
			
			if(_slowStart){ 
				_cwnd++;
				if(_cwnd > _ssthresh){ // cwnd is now beyond the threshold
					_slowStart = false;
				}
			}else{
				// cwnd + (MSS^2 / cwnd)
				_cwnd = (int)(_cwnd + (Math.pow(GBNCCProtocol.MSS, 2) / _cwnd));
			}
			
		}
	}
}
