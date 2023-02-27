#include <iostream>

using namespace std;

double *pha, *phb, *phc;

void OnMult(int m_ar, int m_br)
{
	pha = (double *)malloc((m_ar * m_ar) * sizeof(double));
	phb = (double *)malloc((m_ar * m_ar) * sizeof(double));
	phc = (double *)malloc((m_ar * m_ar) * sizeof(double));
	int i, j, k;
	int temp;

	for (i = 0; i < m_ar; i++)
		for (j = 0; j < m_ar; j++)
			pha[i * m_ar + j] = (double)1.0;

	for (i = 0; i < m_br; i++)
		for (j = 0; j < m_br; j++)
			phb[i * m_br + j] = (double)(i + 1);

	for (i = 0; i < m_ar; i++)
	{
		for (j = 0; j < m_br; j++)
		{
			temp = 0;
			for (k = 0; k < m_ar; k++)
			{
				temp += pha[i * m_ar + k] * phb[k * m_br + j];
			}
			phc[i * m_ar + j] = temp;
		}
	}
}

void OnMultLine(int m_ar, int m_br)
{
	pha = (double *)malloc((m_ar * m_ar) * sizeof(double));
	phb = (double *)malloc((m_ar * m_ar) * sizeof(double));
	phc = (double *)malloc((m_ar * m_ar) * sizeof(double));
	int i, j, k;

	for (i = 0; i < m_ar; i++)
		for (j = 0; j < m_ar; j++)
			pha[i * m_ar + j] = (double)1.0;

	for (i = 0; i < m_br; i++)
		for (j = 0; j < m_br; j++)
			phb[i * m_br + j] = (double)(i + 1);

	for (i = 0; i <= m_ar; i++)
		for (j = 0; j <= m_br; j++)
			phc[i * m_ar + j] = (double)0.0;

	for (i = 0; i < m_ar; i++)
	{
		for (k = 0; k < m_br; k++)
		{
			for (j = 0; j < m_ar; j++)
			{
				phc[i * m_ar + j] += pha[i * m_ar + k] * phb[k * m_br + j];
			}
		}
	}
}

void OnMultBlock(int m_ar, int m_br, int bkSize)
{
	pha = (double *)malloc((m_ar * m_ar) * sizeof(double));
	phb = (double *)malloc((m_ar * m_ar) * sizeof(double));
	phc = (double *)malloc((m_ar * m_ar) * sizeof(double));
	int i, j, k, x, y, z;

	for (i = 0; i < m_ar; i++)
		for (j = 0; j < m_ar; j++)
			pha[i * m_ar + j] = (double)1.0;

	for (i = 0; i < m_br; i++)
		for (j = 0; j < m_br; j++)
			phb[i * m_br + j] = (double)(i + 1);

	for (i = 0; i <= m_ar; i++)
		for (j = 0; j <= m_br; j++)
			phc[i * m_ar + j] = (double)0.0;

	for (i = 0; i < m_ar; i += bkSize)
	{
		for (j = 0; j < m_br; j += bkSize)
		{
			for (k = 0; k < m_ar; k += bkSize)
			{
				for (x = i; x < min(i + bkSize, m_ar); x++)
				{
					for (y = k; y < min(k + bkSize, m_br); y++)
					{
						for (z = j; z < min(j + bkSize, m_ar); z++)
						{
							phc[x * m_ar + z] += pha[x * m_ar + y] * phb[y * m_br + z];
						}
					}
				}
			}
		}
	}
}

void checkResult(int m_ar, int m_br)
{
	cout << "Result matrix: " << endl;
	for (int i = 0; i < m_ar; i++)
	{
		cout << endl;
		for (int j = 0; j < min(10, m_br); j++)
			cout << phc[j] << " ";
	}
	cout << endl;

	free(pha);
	free(phb);
	free(phc);
}

int main()
{
	cout << "1 - On Mult" << endl
		 << "2- On mult line" << endl
		 << "3- on mult block" << endl;
	int opt;
	cin >> opt;
	int x = 3, y = 3;
	switch (opt)
	{
	case 1:
	{
		OnMult(x, y);
		break;
	}
	case 2:
	{
		OnMultLine(x, y);
		break;
	}
	case 3:
	{
		OnMultBlock(x, y, 3);
		break;
	}
	default:
		break;
	}
	checkResult(x, y);
	return 0;
}