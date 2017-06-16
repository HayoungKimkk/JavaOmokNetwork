package kr.ac.kw.robotics.omok;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Random;

public class OmokServer{

	final static private int EMPTY = 0;
	final static private int PLAYER_BLACK = 1;
	final static private int PLAYER_WHITE = 2;
	
	final static private int PLAYER_MAX_NUMBER = 2;
	
	final static private String[] PLAYER_STRING = {"Player 1","Player 2"}; 
	
	
	private static ServerSocket serverSocket;
	
	private static Socket[] playerSocket = new Socket[PLAYER_MAX_NUMBER];
	
	private static DataInputStream[] dis = new DataInputStream[PLAYER_MAX_NUMBER];
	private static DataOutputStream[] dos = new DataOutputStream[PLAYER_MAX_NUMBER];
	
	private static int playerOrder; 
	private static boolean isFinished = false;
	
	private static OmokMap omokMap = new OmokMap(); 
	
	public static class OmokMap
	{
		public static final int COLS = 15;
		public static final int ROWS = 15;

		private static int map[][];
		
		public OmokMap()
		{
			map = new int[ROWS][];
			for (int i = 0; i < ROWS; i++) 
			{
				map[i] = new int[COLS];
			}
			
			for (int i = 0; i < ROWS; i++) 
			{
				for (int j = 0; j < COLS; j++)
				{
					map[i][j] = 0;
				}
			}
		}
		
		public void setMap(int col,int row, int stone)
		{
			if(col>=0 && col<COLS)
				if(row>=0 && row<ROWS)
				{
					map[col][row] = stone;
				}
		}
		
		public int getMap(int col,int row)
		{
			if(col>=0 && col<COLS)
				if(row>=0 && row<ROWS)
				{
					return map[col][row];
				}
			
			return -1;
		}
	}
	
	public OmokServer(int port)
	{
		try
		{
			serverSocket = new ServerSocket(port);
			serverSocket.setSoTimeout(0);
		}
		catch (SocketException e)
		{
			System.out.println("[s] Ÿ�Ӿƿ� ���� ����");
		}
		catch (IOException e)
		{
			System.out.println("[s] ���� ���� ����");
		}
	}
	
	
	public static void main(String[] args)
	{
		try
		{
			int port = Integer.parseInt(args[0]);
			
			OmokServer omokserver = new OmokServer(port);
			
			
			//�浹, �鵹 �� ������ ��ٸ�
			for(int playerIdx = 0; playerIdx<PLAYER_MAX_NUMBER ; playerIdx++)
			{
				try
				{
					System.out.println("/************************************/");
					System.out.println(PLAYER_STRING[playerIdx] + " �� ��ٸ��� ��....");
				
					playerSocket[playerIdx] = serverSocket.accept();
					
					dis[playerIdx] = new DataInputStream (playerSocket[playerIdx].getInputStream());
					dos[playerIdx] = new DataOutputStream (playerSocket[playerIdx].getOutputStream());
					
					System.out.println(PLAYER_STRING[playerIdx] + " �� ���ӿϷ� !");
					System.out.println("/************************************/");
				}
				catch (IOException e) 
				{
					e.printStackTrace();
				}
			}
			
			setOrder();
			play();
			
		
			for(Socket socket : playerSocket)
			{
				try 
				{
					socket.close();
				} 
				catch (IOException e) 
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		catch(NumberFormatException e)
		{
			System.out.println("[ERROR]: �Էµ� ��Ʈ�� ������ �ùٸ��� �ʽ��ϴ�. Ex: 7000");
		}
		catch(ArrayIndexOutOfBoundsException e)
		{
			System.out.println("[ERROR]: ��Ʈ�� �Է����ּ���.  Ex: 7000");
		}

	}
	
	public static void setOrder()
	{
		Random random = new Random(System.currentTimeMillis());
		
		playerOrder = Math.abs(random.nextInt())%PLAYER_MAX_NUMBER;
		System.out.println(PLAYER_STRING[playerOrder] + " �� ����!");
		
		for(int playerIdx = 0; playerIdx<PLAYER_MAX_NUMBER ; playerIdx++)
		{
			try 
			{
				dos[playerOrder].writeUTF("SET_ORDER");
				dos[playerOrder].writeInt(playerIdx + 1);//�� ���� ���� (�� �� ����)
				dos[playerOrder].flush();
			} 
			catch (IOException e) 
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			playerOrder = (playerOrder+1)%PLAYER_MAX_NUMBER;
		}
	}
	
	public static void play()
	{
		while(true)
		{
			int nowPlayer  = playerOrder;
			int nextPlayer = (playerOrder+1)%PLAYER_MAX_NUMBER;
			
			try 
			{
				dos[nowPlayer].writeUTF(PLAYER_STRING[nowPlayer] + "�� �����Դϴ�.(Your turn)");
				dos[nextPlayer].writeUTF(PLAYER_STRING[nowPlayer] + "�� �����Դϴ�.(Enemy turn)");

				dos[nowPlayer].writeUTF("YOUR_TURN");
				
				String strTurn = null;
				
				while(strTurn == null) 
				{
					 strTurn = dis[nowPlayer].readUTF();
				}
				
				
				
				switch(strTurn)
				{
				case "CHANCE":
					do
					{
						updateStone(nowPlayer,nextPlayer);
						strTurn = dis[nowPlayer].readUTF();
					}while(!strTurn.equals("NEXT"));
					
				case "NEXT":
					updateStone(nowPlayer,nextPlayer);	
				break;
				}
				
				if(isFinished == true)
				{
					System.out.println("�¸�: " + PLAYER_STRING[nowPlayer] );
					
					dos[nowPlayer].writeUTF("MATCH_FINISH");
					dos[nextPlayer].writeUTF("MATCH_FINISH");
					
					dos[nowPlayer].writeUTF(PLAYER_STRING[nowPlayer] + " �� �¸��Դϴ�.(You are Winner.)");
					dos[nextPlayer].writeUTF(PLAYER_STRING[nowPlayer] + " �� �¸��Դϴ�.(You are Loser.)");
					
					break;
				}
			} 
			catch (IOException e) 
			{
				e.printStackTrace();
				break;
			}
			playerOrder = (playerOrder+1)%PLAYER_MAX_NUMBER;
		}
		
		for(Socket s :playerSocket)
		{
			try 
			{
				s.close();
			} 
			catch (IOException e) 
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		System.out.println("������ ����˴ϴ�.");
	}

	public static void updateStone(int nowPlayer, int nextPlayer)
	{
		try 
		{
			int col = dis[nowPlayer].readInt();
			int row = dis[nowPlayer].readInt();
			int stone = dis[nowPlayer].readInt();
			
			omokMap.setMap(col, row, stone);
			
			for(int i = 0; i<OmokMap.ROWS; i++)
			{
				for(int j = 0; j<OmokMap.COLS; j++)
				{
					if(omokMap.getMap(i, j) == PLAYER_BLACK)
					{
						System.out.print(" O ");
					}
					else if(omokMap.getMap(i, j) == PLAYER_WHITE)
					{
						System.out.print(" X ");
					}
					else
					{
						System.out.print(" = ");
					}
				}
				System.out.println("");
			}
			
			System.out.println("[����] " +PLAYER_STRING[nowPlayer] + " : col,row(" + col + ", " + row +") stone = " + stone);
			
			
			dos[nextPlayer].writeUTF("UPDATE_STONE");
			
			dos[nextPlayer].writeInt(col);
			dos[nextPlayer].writeInt(row);
			dos[nextPlayer].writeInt(stone);
			
			dos[nextPlayer].flush();
			
			System.out.println("[����] " +PLAYER_STRING[nextPlayer] + " : col,row(" + col + ", " + row +") stone = " + stone);
			
			if(stone != EMPTY)
			{
				isFinished = checkOmok(col, row, stone);
			}
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}

	}
	
	public static boolean checkOmok(int col, int row, int stone)
	{
		//4 ���� �˻�.
		
		//1. ���� ���� ( 0�� ,180 ��)
		for(int startRow = row - 4; startRow<= row; startRow++)
		{
			int nPoint  = 0;
			
			for(int moveRow = startRow; moveRow<= startRow+4; moveRow++)
			{
				if(omokMap.getMap(col, moveRow) == stone)
				{
					nPoint++;
				}
				else
				{
					break;
				}
			}
			
			if(nPoint == 5)
			{
				return true;
			}
		}
		//2. ���� ���� 
		for(int startCol = col - 4; startCol<= col; startCol++)
		{
			int nPoint  = 0;
			
			for(int moveCol = startCol; moveCol<= startCol+4; moveCol++)
			{
				if(omokMap.getMap(moveCol, row) == stone)
				{
					nPoint++;
				}
				else
				{
					break;
				}
			}
			
			if(nPoint == 5)
			{
				return true;
			}
		}
		//3. �� -> �� �ϰ�  �밢�� ����
		for(int nStartMove = -4; nStartMove <= 0; nStartMove++)
		{
			int startRow = row + nStartMove;
			int startCol = col + nStartMove;
			
			int nPoint  = 0;
			
			for(int nEndMove = 0; nEndMove <= 4; nEndMove++)
			{
				int moveRow = startRow + nEndMove;
				int moveCol = startCol + nEndMove;
				
				if(omokMap.getMap(moveCol, moveRow) == stone)
				{
					nPoint++;
				}
				else
				{
					break;
				}
			}

			if(nPoint == 5)
			{
				return true;
			}
		}
		//4. �� -> �� �ϰ� �밢�� ����
		for(int nStartMove = -4; nStartMove <= 0; nStartMove++)
		{
			int startRow = row + nStartMove;
			int startCol = col - nStartMove;
			
			int nPoint  = 0;
			
			for(int nEndMove = 0; nEndMove <= 4; nEndMove++)
			{
				int moveRow = startRow + nEndMove;
				int moveCol = startCol - nEndMove;
				
				if(omokMap.getMap(moveCol, moveRow) == stone)
				{
					nPoint++;
				}
				else
				{
					break;
				}
			}

			if(nPoint == 5)
			{
				return true;
			}
		}
		
		return false;
	}
}
